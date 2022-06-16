package com.example.moodmemustache.video_screen;

public class Mustache {
    //stores data about each mustache/filter
    /*imagePath = mustache that is displayed in scrollable list at bottom
    modelPath and texturePath describe model and texture used for this particular mustache
     */
    String imagePath, modelPath, texturePath;

    public Mustache(String im, String m, String t){
        imagePath = im;
        modelPath = m;
        texturePath = t;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }
}
