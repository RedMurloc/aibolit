package com.aibolit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import net.sourceforge.tess4j.*;

import javax.imageio.ImageIO;

import static net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode.*;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;

public class AibolitMain {
    public static void main(String[] args) {
        // System.setProperty("jna.library.path", "32".equals(System.getProperty("sun.arch.data.model")) ? "lib/win32-x86" : "lib/win32-x86-64");

        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("test6.png"));
        } catch (IOException e) {
        }

        //File imageFile = new File("test2.png");
        ITesseract instance = new Tesseract1();  // JNA Interface Mapping
        //instance.setTessVariable("tessedit_char_whitelist", "0123456789.");
        //instance.setLanguage("rus");
        instance.setLanguage("eng");
        instance.setOcrEngineMode(OEM_TESSERACT_LSTM_COMBINED);
        //instance.setPageSegMode(PSM_SINGLE_BLOCK);
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        // File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Maven build bundles English data
        // instance.setDatapath(tessDataFolder.getPath());

        try {
            //String result = instance.doOCR(img, new Rectangle(355, 347, 61, 28));
            String result = instance.doOCR(img);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }
}