package com.example.moodmemustache.video_screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmemustache.R;
import com.example.moodmemustache.recording_screen.RecordingsPage;
import com.example.moodmemustache.video_db.Video;
import com.example.moodmemustache.video_db.VideoDatabase;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFrontFacingFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CreateVideo extends AppCompatActivity implements View.OnClickListener, TagDialog.TagDialogListener, MustacheAdapter.OnMustacheClick {

    private final String TAG = "Error in ARCore";
    //permissions needed
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private final int AUDIO_PERMISSION = 102;
    private final int VIDEO_PERMISSION = 100;

    //keep track of all mustaches and their attributes
    private final int TOTAL_MUSTACHES = 7;
    private final String[] TEXTURES = new String[]{"blank.png", "black_mustache.png", "brown_mustache.png", "pink_mustache.png", "rainbow_mustache.png", "freckles.png", "fox_face_mesh_texture.png"};
    private final String[] MODELS = new String[]{"face.glb", "face.glb", "face.glb", "face.glb", "face.glb", "face.glb", "fox.glb"};
    private final String[] IMAGES = new String[]{"blank.png", "black_mustache.png", "brown_mustache.png", "pink_mustache.png", "rainbow_mustache.png", "freckles.png", "fox_face.png"};

    //keep track of whether currently recording
    private boolean startedRecording = false;
    //keep track of whether audio permission has been given
    private boolean audioAllowed;
    private final Set<CompletableFuture<?>> loaders = new HashSet<>();

    //the sceneview and fragment
    private ArFrontFacingFragment arFragment;
    private ArSceneView arSceneView;

    //the recyclerview list of mustaches/filters the user can choose from
    private RecyclerView mustacheList;
    private List<Mustache> mustaches;
    private MustacheAdapter adapter;

    //the current texture and model being used for the face
    private Texture faceTexture;
    private ModelRenderable faceModel;

    //record video and audio separately
    private VideoRecorder recorder;
    private File audiofile;
    private MediaRecorder audioRecorder;

    private final HashMap<AugmentedFace, AugmentedFaceNode> facesNodes = new HashMap<>();
    private AugmentedFaceNode faceNode;
    //the started recording/end recording button
    private Button recordingToggle;
    private ImageButton goBack;
    //filname -> file for preview image
    //outputFile -> file that final video+audio is stored in
    private String filename, outputFile;
    private int duration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_video);
        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this) && isARCoreSupportedAndUpToDate()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFrontFacingFragment.class, null)
                        .commit();
            }else{
                Toast.makeText(this, "ARCore and Sceneform are not supported", Toast.LENGTH_LONG);
            }
        }
        //ask for permission to record
        audioAllowed = hasPermissions(REQUIRED_PERMISSIONS[1]);
        if (!hasPermissions(REQUIRED_PERMISSIONS[0])) {
            ActivityCompat.requestPermissions(this, new String[] {REQUIRED_PERMISSIONS[0]}, VIDEO_PERMISSION);
        }

        goBack = (ImageButton) findViewById(R.id.return_button);
        mustacheList = (RecyclerView) findViewById(R.id.mustache_list);
        mustaches = new ArrayList<Mustache>();
        //create mustache objects and pass into recyclerview to display
        for (int i = 0; i<TOTAL_MUSTACHES; i++){
            mustaches.add(new Mustache(IMAGES[i], MODELS[i], TEXTURES[i]));
        }

        adapter = new MustacheAdapter(this, mustaches, this);
        mustacheList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mustacheList.setAdapter(adapter);

        goBack.setOnClickListener(this);
        recordingToggle = (Button) findViewById(R.id.recording_toggle);
        recordingToggle.setOnClickListener(this);
        recordingToggle.setBackgroundColor(Color.GREEN);

        //if audio permission has not been given, first ask for that permission
        if (!audioAllowed){
            recordingToggle.setText("Allow audio");
        }
    }

    //check if specified permission is granted
    public boolean hasPermissions(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==VIDEO_PERMISSION) {//check if video permission has been granted
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {//if not, return to previous activity
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_LONG);
                Intent intent = new Intent(this, RecordingsPage.class);
                startActivity(intent);
            }
        }else if (requestCode == AUDIO_PERMISSION){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {//if audio is allowed, allow recording to begin
                recordingToggle.setText(R.string.start_recording);
                audioAllowed = true;
            } else {//if not, return to previous activity
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_LONG);
                Intent intent = new Intent(this, RecordingsPage.class);
                startActivity(intent);
            }
        }
    }

    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFrontFacingFragment) fragment;
            arFragment.setOnViewCreatedListener(this::onViewCreated);
        }
    }

    //
    public void onViewCreated(ArSceneView arSceneView) {
        this.arSceneView = arSceneView;

        arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        //check for face detections
        arFragment.setOnAugmentedFaceUpdateListener(this::onAugmentedFaceTrackingUpdate);

        //set up recorder
        recorder = new VideoRecorder(this);
        recorder.setSceneView(arFragment.getArSceneView());

        int orientation = getResources().getConfiguration().orientation;
        recorder.setVideoQuality(CamcorderProfile.QUALITY_480P, orientation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (CompletableFuture<?> loader : loaders) {
            if (!loader.isDone()) {
                loader.cancel(true);
            }
        }
    }

    public void startAudio() {
        //create file
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            audiofile = File.createTempFile("sound", ".mp4", dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //create mediarecorder and specify attributes
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        audioRecorder.setOutputFile(audiofile.getAbsolutePath());
        try {
            audioRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //start recording audio
        audioRecorder.start();
    }

    //stop recording audio
    public void stopAudio() {
        audioRecorder.stop();
        audioRecorder.release();
    }

    //generate UNIQUE filename for preview image of video
    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + "IM/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {
        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    //take a photo of the current scene to save as the preview image in recordings list
    private void takePhoto(){
        filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView();

        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),view.getHeight(),
                Bitmap.Config.ARGB_8888);

        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(view, bitmap, (copyResult) -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        saveBitmapToDisk(bitmap, filename);

                        Uri uri = Uri.parse("file://" + filename);
                        Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        i.setData(uri);
                        sendBroadcast(i);

                    } catch (IOException e) {
                        Toast toast = Toast.makeText(this, e.toString(),
                                Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                }
                handlerThread.quitSafely();
            }, new Handler(handlerThread.getLooper()));
        }
    }

    //load the specified model onto the face
    private void loadModels(String modelPath) {
        //get rid of current models
        facesNodes.clear();
        try{
            faceNode.setParent(null);
        }
        catch (Exception e){
        }
        faceModel = null;
        faceTexture = null;

        //load new model
        loaders.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/"+modelPath))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> faceModel = model)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                    return null;
                }));
    }

    //load specified texture onto face
    private void loadTextures(String texturePath, boolean isBlank) {
        if (isBlank){//if user clicks on blank texture, then don't load anything
            loaders.add(null);
            return;
        }
        //otherwise, load the specified texture
        loaders.add(Texture.builder()
                .setSource(this, Uri.parse("textures/"+texturePath))
                .setUsage(Texture.Usage.COLOR_MAP)
                .build()
                .thenAccept(texture -> faceTexture = texture)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load texture", Toast.LENGTH_LONG).show();
                    return null;
                }));
    }

    //action to take when face is detected or needs to be updated
    public void onAugmentedFaceTrackingUpdate(AugmentedFace augmentedFace) {
        if (faceModel == null || faceTexture == null) {
            return;
        }

        AugmentedFaceNode existingFaceNode = facesNodes.get(augmentedFace);

        switch (augmentedFace.getTrackingState()) {
            case TRACKING:
                if (existingFaceNode == null) {
                    faceNode = new AugmentedFaceNode(augmentedFace);

                    RenderableInstance modelInstance = faceNode.setFaceRegionsRenderable(faceModel);
                    modelInstance.setShadowCaster(false);
                    modelInstance.setShadowReceiver(true);

                    faceNode.setFaceMeshTexture(faceTexture);

                    arSceneView.getScene().addChild(faceNode);

                    facesNodes.put(augmentedFace, faceNode);
                }
                break;
            case STOPPED:
                if (existingFaceNode != null) {
                    arSceneView.getScene().removeChild(existingFaceNode);
                }
                facesNodes.remove(augmentedFace);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.recording_toggle){//click on recording button
            if (!startedRecording){//recording has not been started yet
                if (!audioAllowed){//ask for audio
                    ActivityCompat.requestPermissions(this, new String[] {REQUIRED_PERMISSIONS[1]}, AUDIO_PERMISSION);
                }else{//otherwise, begin recording
                    //do not allow user to go back until recording is finished
                    goBack.setEnabled(false);
                    startedRecording = true;
                    //start video and audio
                    recorder.onToggleRecord();
                    startAudio();
                    //change button text and color
                    recordingToggle.setText(R.string.end_recording);
                    recordingToggle.setBackgroundColor(Color.RED);
                }
            }else{//if recording has been started, then just end it
                //take a photo for preview image of video
                takePhoto();
                goBack.setEnabled(true);
                //end video and audio
                recorder.onToggleRecord();
                stopAudio();
                //combine the video and audio file into one .mp4 file
                muxing();
                //collect duration and tag to display
                duration = getDuration(outputFile);
                showTagDialog();
                startedRecording = false;
                recordingToggle.setText(R.string.start_recording);
                recordingToggle.setBackgroundColor(Color.GREEN);
            }
        }else{//return to recordings page
            Intent intent = new Intent(this, RecordingsPage.class);
            startActivity(intent);
        }
    }

    //create new TagDialog
    public void showTagDialog() {
        DialogFragment dialog = new TagDialog();
        dialog.show(getSupportFragmentManager(), "TagDialog");
    }

    @Override
    public void onDialogPositiveClick(String tag) {
        //load new video into ORM asynchronously
        AsyncTask.execute(() -> {
            VideoDatabase appData = VideoDatabase.getInstance(getApplicationContext());
            Video vid = new Video(filename, outputFile, tag, duration);
            appData.videoDao().insertVideo(vid);
        });
    }

    //verify that ARCore is installed and using the current version.
    private boolean isARCoreSupportedAndUpToDate() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        switch (availability) {
            case SUPPORTED_INSTALLED:
                return true;

            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try {
                    //request ARCore installation or update if needed.
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this, true);
                    switch (installStatus) {
                        case INSTALL_REQUESTED:
                            Log.i(TAG, "ARCore installation requested.");
                            return false;
                        case INSTALLED:
                            return true;
                    }
                } catch (UnavailableException e) {
                    Log.e(TAG, "ARCore not installed", e);
                }
                return false;

            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                //this device is not supported for AR.
                return false;

            case UNKNOWN_CHECKING:
                //ARCore is checking the availability with a remote query.
                //this function should be called again after waiting 200 ms to determine the query result
                try {
                    wait(200);
                    isARCoreSupportedAndUpToDate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            case UNKNOWN_ERROR:
                return false;
            case UNKNOWN_TIMED_OUT:
                return false;
            default:
                return false;
        }
    }

    //combine both video and audio file into one .mp4 file
    private void muxing() {

        //create output file
        outputFile = "";
        try {
            String date =
                    new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + date +"finalmixed.mp4");
            file.createNewFile();
            outputFile = file.getAbsolutePath();

            //use media extractors for both video and audio
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(recorder.getVideoPath().getAbsolutePath());

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audiofile.getAbsolutePath());

            MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            int videoTrack = muxer.addTrack(videoFormat);

            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
            int audioTrack = muxer.addTrack(audioFormat);

            //set up the media format and extractors
            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer.start();

            //go through video and audio in separate loops and combine them
            while (!sawEOS)
            {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);


                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    sawEOS = true;
                    videoBufferInfo.size = 0;

                }
                else
                {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();


                    frameCount++;

                }
            }


            boolean sawEOS2 = false;
            int frameCount2 =0;
            while (!sawEOS2)
            {
                frameCount2++;

                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    sawEOS2 = true;
                    audioBufferInfo.size = 0;
                }
                else
                {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                    audioExtractor.advance();

                }
            }

            muxer.stop();
            muxer.release();


        } catch (IOException e) {
            Log.d("MUXING ERROR", "Mixer Error 1 " + e.getMessage());
        } catch (Exception e) {
            Log.d("MUXING ERROR", "Mixer Error 2 " + e.getMessage());
        }
    }

    //get duration of video with specified path
    private int getDuration(String pathStr) {
        Uri uri = Uri.parse(pathStr);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr);
    }

    //when mustache is clicked in list, load that mustache onto face
    @Override
    public void onMustacheClickEdit(int pos) {
        Mustache curMustache = mustaches.get(pos);
        boolean isBlank = (pos==0);
        loadModels(curMustache.getModelPath());
        loadTextures(curMustache.getTexturePath(), isBlank);
    }
}
