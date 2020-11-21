package com.aibolit;

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
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

    static final String tscore = "t-score";

    public static int levenstain(String str1, String str2) {
        int[] Di_1 = new int[str2.length() + 1];
        int[] Di = new int[str2.length() + 1];

        for (int j = 0; j <= str2.length(); j++) {
            Di[j] = j; // (i == 0)
        }

        for (int i = 1; i <= str1.length(); i++) {
            System.arraycopy(Di, 0, Di_1, 0, Di_1.length);

            Di[0] = i; // (j == 0)
            for (int j = 1; j <= str2.length(); j++) {
                Di[j] = min(
                        Di_1[j] + 1,
                        Di[j - 1] + 1,
                        Di_1[j - 1] + ((str1.charAt(i - 1) != str2.charAt(j - 1)) ? 1 : 0)
                );
            }
        }

        return Di[Di.length - 1];
    }

    public static boolean isTscoreCandidate(Word word) {
        String text = word.getText().trim().toLowerCase();
        return !text.equals(tscore) && !text.equals("score") && (levenstain(tscore, text) <= 2);
    }

    private static int min(int n1, int n2, int n3) {
        return Math.min(Math.min(n1, n2), n3);
    }

    private static int getY(Word word) {
        return (int) word.getBoundingBox().getY();
    }

    private static Rectangle getSafeWiderBox(Rectangle box) {
        int width = (int) box.getWidth();
        int height = (int) box.getHeight();
        Rectangle newBox = new Rectangle((int) Math.max(box.getX() - ((int) (width/5)), 0),
                                         (int) Math.max(box.getY() - ((int) (height/5)), 0),
                                         (int) (box.getWidth() * 1.4),
                                         (int) (box.getHeight() * 1.4)
        );
        return newBox;
    }


    public static void main(String[] args) {
        // System.setProperty("jna.library.path", "32".equals(System.getProperty("sun.arch.data.model")) ? "lib/win32-x86" : "lib/win32-x86-64");

        BufferedImage img = null;
        try {
            /*img = ImageIO.read(new File("test21.jpg"));*/
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            img = new Robot().createScreenCapture(screenRect);
        } catch (Exception e) {
            e.printStackTrace();
        }

       /* final ImageInfo imageInfo = Imaging.getImageInfo(file);

        final int physicalWidthDpi = imageInfo.getPhysicalWidthDpi();
        final int physicalHeightDpi = imageInfo.getPhysicalHeightDpi();*/

        //File imageFile = new File("test2.png");
        ITesseract instance = new Tesseract1();  // JNA Interface Mapping
        //instance.setTessVariable("tessedit_char_whitelist", "0123456789.");
        //instance.setLanguage("rus");
        instance.setLanguage("eng");
        //instance.setOcrEngineMode(OEM_TESSERACT_LSTM_COMBINED);
        //instance.setOcrEngineMode(OEM_TESSERACT_ONLY);
        instance.setOcrEngineMode(OEM_LSTM_ONLY);

        //instance.setTessVariable("user_defined_dpi", "92");
        List<Word> words = instance.getWords(img, 3);
        List<Word> tscores = words.stream().filter(it -> it.getText().trim().toLowerCase().equals(tscore)).collect(Collectors.toList());
        List<Word> tscoresCandidates = words.stream().filter(AibolitMain::isTscoreCandidate).collect(Collectors.toList());

        ITesseract instance2 = new Tesseract1();
        instance2.setLanguage("eng");
        instance2.setTessVariable("tessedit_char_whitelist", "Tt-Sscore");
        for(Word candidate: tscoresCandidates) {
            Rectangle box = candidate.getBoundingBox();
            Rectangle newBox = getSafeWiderBox(box);
            BufferedImage biggerImage = img.getSubimage((int) newBox.getX(), (int)  newBox.getY(), (int) newBox.getWidth(), (int)  newBox.getHeight());
            List<Word> candidates = instance2.getWords(biggerImage, 3);
            if (candidates.stream().map(it -> it.getText().trim().toLowerCase()).collect(Collectors.toList()).contains(tscore)) {
                tscores.add(candidate);
            }
        }

        //Word tscore = tscores.get(0);
        int iter = 0;
        for (Word tscore: tscores) {
            Rectangle box = tscore.getBoundingBox();
           /* BufferedImage img1 = img.getSubimage((int) box.getX(), (int) box.getY(), (int) box.getWidth(), (int) (img.getHeight() - box.getY()));
            File outputfile1 = new File("img1.jpg");*/
            BufferedImage subimage = img.getSubimage((int) box.getX(), (int) (box.getY() + box.getHeight()), (int) box.getWidth(), (int) (img.getHeight() - box.getY() - box.getHeight()));
            File debugImage1 = new File("debugImage-" + iter + "-1.jpg");
            /*try {
                ImageIO.write(img1, "jpg", outputfile1);
                ImageIO.write(img2, "jpg", outputfile2);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            instance.setTessVariable("tessedit_char_whitelist", "0123456789.-");
            words = instance.getWords(subimage, 3);
            words.sort(Comparator.comparingInt(AibolitMain::getY));

            int wordsEnd = 0;

            if (words.size() >= 2) {
                if (getY(words.get(1)) - getY(words.get(0)) <= words.get(0).getBoundingBox().getHeight() * 2.5) {
                    wordsEnd = 1;
                    if (words.size() > 2) {
                        int delta = getY(words.get(1)) - getY(words.get(0));
                        for (int i = 2; i < words.size(); ++i) {
                            int currDelta = getY(words.get(i)) - getY(words.get(i - 1));
                            if (delta * 1.2 >= currDelta) {
                                ++wordsEnd;
                            } else break;
                        }
                    }
                }
            }

            File debugImage2 = new File("debugImage-" + iter + "-2.jpg");

            BufferedImage debugImage2buf = subimage.getSubimage(0 , 0, subimage.getWidth(),  (int)(words.get(wordsEnd).getBoundingBox().getY() + words.get(wordsEnd).getBoundingBox().getHeight()));

            try {
                ImageIO.write(subimage, "jpg", debugImage1);
                ImageIO.write(debugImage2buf, "jpg", debugImage2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            words = words.subList(0, wordsEnd + 1);

            System.out.println("==============================");
            System.out.println(words);
            ++iter;
        }
        //instance.setPageSegMode(PSM_SINGLE_BLOCK);
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        // File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Maven build bundles English data
        // instance.setDatapath(tessDataFolder.getPath());

        /*try {
            //String result = instance.doOCR(img, new Rectangle(355, 347, 61, 28));
            String result = instance.doOCR(img2);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }*/
    }
}