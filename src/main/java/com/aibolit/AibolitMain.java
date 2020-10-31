package com.aibolit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.sourceforge.tess4j.*;

import javax.imageio.ImageIO;

import static net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode.*;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;

public class AibolitMain {

    private static int getY(Word word) {
        return (int) word.getBoundingBox().getY();
    }

    public static void main(String[] args) {
        // System.setProperty("jna.library.path", "32".equals(System.getProperty("sun.arch.data.model")) ? "lib/win32-x86" : "lib/win32-x86-64");

        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("test21.jpg"));
        } catch (IOException e) {
        }

        //File imageFile = new File("test2.png");
        ITesseract instance = new Tesseract1();  // JNA Interface Mapping
        //instance.setTessVariable("tessedit_char_whitelist", "0123456789.");
        //instance.setLanguage("rus");
        instance.setLanguage("eng");
        instance.setOcrEngineMode(OEM_TESSERACT_LSTM_COMBINED);
        List<Word> words = instance.getWords(img, 3);
        List<Word> tscores = words.stream().filter(it -> it.getText().equals("T-score")).collect(Collectors.toList());
        Word tscore = tscores.get(0);
        Rectangle box = tscore.getBoundingBox();
        BufferedImage img1 = img.getSubimage((int) box.getX(), (int) box.getY(), (int) box.getWidth(), (int) (img.getHeight() - box.getY()));
        File outputfile1 = new File("img1.jpg");
        BufferedImage img2 = img.getSubimage((int) box.getX(), (int) (box.getY() + box.getHeight()), (int) box.getWidth(), (int) (img.getHeight() - box.getY() - box.getHeight()));
        File outputfile2 = new File("img2.jpg");
        try {
            ImageIO.write(img1, "jpg", outputfile1);
            ImageIO.write(img2, "jpg", outputfile2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        instance.setTessVariable("tessedit_char_whitelist", "0123456789.-");
        words = instance.getWords(img2, 3);
        words.sort(Comparator.comparingInt(o -> getY(o)));
        int wordsEnd = 1;

        if (words.size() == 2) {
            if (getY(words.get(1)) - getY(words.get(0)) <= words.get(0).getBoundingBox().getHeight() * 2) {
                wordsEnd = 2;
            }
        }

        if (words.size() > 2) {
            int delta = getY(words.get(1)) - getY(words.get(0));
            for (int i = 2; i < words.size(); ++i) {
                int currDelta = getY(words.get(i)) - getY(words.get(i-1));
                if (delta * 1.2 >= currDelta) {
                    ++wordsEnd;
                }
                else break;
            }
        }

        words = words.subList(0, wordsEnd + 1);

        System.out.println(words);

        //instance.setPageSegMode(PSM_SINGLE_BLOCK);
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        // File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Maven build bundles English data
        // instance.setDatapath(tessDataFolder.getPath());

        try {
            //String result = instance.doOCR(img, new Rectangle(355, 347, 61, 28));
            String result = instance.doOCR(img2);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }
}