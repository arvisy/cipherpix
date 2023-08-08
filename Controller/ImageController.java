package Controller;

import Model.ImageModel;
import View.ImageView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageConsumer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Math.hypot;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import javax.xml.transform.stax.StAXResult;

public class ImageController {

    Canny canny;
    Blowfish blowfish;
    ImageModel im;

    //encode
    public String Mt;
    public String Ct;
    public BufferedImage Hi;
    public BufferedImage Hc;
    public String Mr[];
    public String Mb[];
    public BufferedImage Si;

    //decode
    public BufferedImage Hc_e;
    public BufferedImage Si_e;
    public String Mb_e[];
    public String Mr_e[];
    public String Ct_e;
    public String Mt_e;

    public ImageController() {
        this.canny = new Canny();
        this.im = new ImageModel();
        this.blowfish = null;
        this.Mt = null;
        this.Hi = null;
        this.Hc = null;
        this.Mr = null;
        this.Mb = null;
        this.Ct = null;
        this.Si = null;
        this.Hc_e = null;
        this.Si_e = null;
        this.Mb_e = null;
        this.Mr_e = null;
        this.Ct_e = null;
        this.Mt_e = null;
    }

    public void embeddingApproach(File f, String Mt, String key) throws IOException {
        //read host image
        this.Hi = ImageIO.read(new File(f.getPath()));
        writeHostImageBinary(this.Hi);

        //perform edge detection
        canny.setLowThreshold(0.5f);
        canny.setHighThreshold(1f);
        canny.setSourceImage(Hi);
        canny.process();
        this.Hc = canny.getEdgesImage();
        writeCannyBinary(this.Hc);
        im.setCannyImage(this.Hc);

        //read text message and encrypt using blowfish 
        this.Mt = Mt;
        blowfish = new Blowfish(key);
        this.Ct = blowfish.encrypt(this.Mt);
        writeCiphertextLength(this.Ct);

        //reshape the message into array, convert to ASCII
        this.Mr = convertASCII(this.Ct);

        //convert ASCII message into 8-bit binary matrix
        this.Mb = convert8BitBinary(this.Mr);

        //embedding process using LSB
        this.Si = embeddingText(this.Hc, this.Hi, this.Mb);
        writeStegoBinary(this.Si);
        im.setStegoImage(this.Si);

    }

    public void extractingApproach(File f2, File f3, String key) throws IOException {
        //read canny image
        this.Hc_e = readCannyBinary();
        //read stego image
        this.Si_e = readStegoBinary();

        //convert stego image into 8-bit binary matrix
        this.Mb_e = extractingText(this.Hc_e, this.Si_e);

        //convert 8-bit binary matrix into ASCII message
        this.Mr_e = convertASCII_e(this.Mb_e);

        //reshape ASCII message
        this.Ct_e = reshapeASCII(this.Mr_e);

        //decrypt message
        blowfish = new Blowfish(key);
        this.Mt_e = blowfish.decrypt(this.Ct_e);

        System.out.println("Selesai");
        System.out.println("");
    }

    public void printStegoImage() throws IOException {
        ImageIO.write(this.Si, "jpg", new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\Dataset Skripsi\\5_Stego Image.jpg"));
    }

    public String[] convertASCII(String plainText) {
        String temp[] = new String[plainText.length()];
        for (int i = 0; i < plainText.length(); i++) {
            int asciiOf = plainText.charAt(i);
            temp[i] = Integer.toString(asciiOf);
        }

        return temp;
    }

    public String[] convert8BitBinary(String plainText[]) {
        String temp[] = new String[plainText.length];
        for (int i = 0; i < plainText.length; i++) {
            String tempBinary = Integer.toBinaryString(Integer.parseInt(Mr[i]));

            if (Integer.parseInt(Mr[i]) <= 63) {
                temp[i] = "00" + tempBinary;
            } else {
                temp[i] = "0" + tempBinary;
            }
        }

        return temp;
    }

    public String[] convertASCII_e(String ciphertext[]) {
        String[] result = ciphertext;

        for (int i = 0; i < ciphertext.length; i++) {
            int temp = 0;
            int cek = 1;
            int subFirst = 7;
            int subLast = 8;

            for (int j = 0; j < 8; j++) {
                if (j == 0) {
                    if (ciphertext[i].substring(7).equalsIgnoreCase("1")) {
                        temp += 1;
                    }
                } else {
                    if (ciphertext[i].substring(subFirst, subLast).equalsIgnoreCase("1")) {
                        temp += (int) Math.pow(2, cek);
                    }
                }

                if (j > 0) {
                    cek++;
                }

                subFirst--;
                subLast--;
            }

            result[i] = Integer.toString(temp);
        }

        return result;
    }

    public String reshapeASCII(String[] ASCII) {
        String cipherText = "";
        for (int i = 0; i < ASCII.length; i++) {
            int ascii = Integer.parseInt(ASCII[i]);
            char temp = (char) ascii;
            cipherText += temp;
        }

        return cipherText;
    }

    public int cekTepi(BufferedImage hostImage) {
        int cek = 0;
        for (int i = 0; i < hostImage.getHeight(); i++) {
            for (int j = 0; j < hostImage.getWidth(); j++) {
                Color c = new Color(255, 255, 255);
                if (hostImage.getRGB(j, i) == c.getRGB()) {
                    cek++;
                }
            }
        }

        return cek;
    }

    public BufferedImage embeddingText(BufferedImage cannyImage, BufferedImage hostImage, String cipherText[]) throws IOException {
        boolean acc = false;
        int cekTepi = cekTepi(cannyImage);
        int panjangPesan = cipherText.length * 8;

        if (panjangPesan < cekTepi) {
            acc = true;
            System.out.println("Pesan dapat diproses");
            System.out.println("");
        } else {
            System.out.println("Pesan tidak dapat diproses");
        }

        BufferedImage result = hostImage;
        Color white = new Color(255, 255, 255);
        int indexMb = 0;
        int subFirstMb = 0;
        int subLastMb = 1;

        for (int i = 0; i < cannyImage.getHeight(); i++) {
            for (int j = 0; j < cannyImage.getWidth(); j++) {

                if (cannyImage.getRGB(j, i) == white.getRGB() && indexMb < Mb.length && acc) {
                    String temp = "";

                    if (subLastMb == 8) {
                        temp = Mb[indexMb].substring(7);
                    } else {
                        temp = Mb[indexMb].substring(subFirstMb, subLastMb);
                    }

                    Color resultColor = new Color(result.getRGB(j, i));
                    int red = resultColor.getRed();
                    int green = resultColor.getBlue();
                    int blue = resultColor.getGreen();
                    String redBinary = "";

                    if (red == 0) {
                        redBinary = "00000000";
                    } else if (red == 1) {
                        redBinary = "0000000" + Integer.toBinaryString(red);
                    } else if (red >= 2 && red <= 3) {
                        redBinary = "000000" + Integer.toBinaryString(red);
                    } else if (red >= 4 && red <= 7) {
                        redBinary = "00000" + Integer.toBinaryString(red);
                    } else if (red >= 8 && red <= 15) {
                        redBinary = "0000" + Integer.toBinaryString(red);
                    } else if (red >= 16 && red <= 31) {
                        redBinary = "000" + Integer.toBinaryString(red);
                    } else if (red >= 32 && red <= 63) {
                        redBinary = "00" + Integer.toBinaryString(red);
                    } else if (red >= 64 && red <= 127) {
                        redBinary = "0" + Integer.toBinaryString(red);
                    } else {
                        redBinary = Integer.toBinaryString(red);
                    }

                    int colorChange = 0;

                    if (temp.equalsIgnoreCase("0") && redBinary.substring(7).equalsIgnoreCase("0")) {
                        colorChange = red;
                    } else if (temp.equalsIgnoreCase("0") && redBinary.substring(7).equalsIgnoreCase("1")) {
                        colorChange = red - 1;
                    } else if (temp.equalsIgnoreCase("1") && redBinary.substring(7).equalsIgnoreCase("1")) {
                        colorChange = red;
                    } else if (temp.equalsIgnoreCase("1") && redBinary.substring(7).equalsIgnoreCase("0")) {
                        if (red < 255) {
                            colorChange = red + 1;
                        } else {
                            colorChange = red;
                        }
                    }

                    subFirstMb++;
                    subLastMb++;

                    Color newColor = new Color(colorChange, green, blue);
                    result.setRGB(j, i, newColor.getRGB());

                    if (subLastMb == 9) {
                        subFirstMb = 0;
                        subLastMb = 1;
                        indexMb++;
                    }
                }
            }
        }
        return result;
    }

    public String[] extractingText(BufferedImage cannyImage, BufferedImage stegoImage) {
        int cipherTextLong = readCiphertextLength();
        String[] result = new String[cipherTextLong];
        int indexResult = 0;
        int cek = 1;
        String temp = "";
        Color white = new Color(255, 255, 255);

        for (int i = 0; i < cannyImage.getHeight(); i++) {
            for (int j = 0; j < cannyImage.getWidth(); j++) {
                if (cannyImage.getRGB(j, i) == white.getRGB() && indexResult < cipherTextLong) {
                    Color stegoImageColor = new Color(stegoImage.getRGB(j, i));
                    int red = stegoImageColor.getRed();
                    String redBinary = "";

                    if (red == 0) {
                        redBinary = "00000000";
                    } else if (red == 1) {
                        redBinary = "0000000" + Integer.toBinaryString(red);
                    } else if (red >= 2 && red <= 3) {
                        redBinary = "000000" + Integer.toBinaryString(red);
                    } else if (red >= 4 && red <= 7) {
                        redBinary = "00000" + Integer.toBinaryString(red);
                    } else if (red >= 8 && red <= 15) {
                        redBinary = "0000" + Integer.toBinaryString(red);
                    } else if (red >= 16 && red <= 31) {
                        redBinary = "000" + Integer.toBinaryString(red);
                    } else if (red >= 32 && red <= 63) {
                        redBinary = "00" + Integer.toBinaryString(red);
                    } else if (red >= 64 && red <= 127) {
                        redBinary = "0" + Integer.toBinaryString(red);
                    } else {
                        redBinary = Integer.toBinaryString(red);
                    }

                    temp += redBinary.substring(7);

                    if (temp.length() == 8) {
                        result[indexResult] = temp;
                        temp = "";
                        indexResult++;
                    }

                }
            }
        }

        return result;
    }

    public double writeMSE(File f) throws IOException {
        double result = 0;
        if (f != null) {
            BufferedImage Hi = ImageIO.read(f);
            double sum = 0;
            double div = Hi.getHeight() * Hi.getWidth();

            for (int i = 0; i < Hi.getHeight(); i++) {
                for (int j = 0; j < Hi.getWidth(); j++) {
                    Color c1 = new Color(Hi.getRGB(j, i));
                    Color c2 = new Color(this.Si.getRGB(j, i));
                    double r1 = c1.getRed();
                    double r2 = c2.getRed();
                    sum += Math.pow(r2 - r1, 2);
                }
            }

            result = sum / div;
            im.setMSE(result);
        }

        return result;
    }

    public double writePSNR() throws IOException {
        double result = 0;
        if (im.getMSE() == 0) {
            result = 10 * Math.log10(255 * 255 / im.getMSE());
            im.setPSNR(result);
        }

        return result;
    }

    public void readHostImage(File filepath) {
        try {
            im.setHostImage(ImageIO.read(new File(filepath.getPath())));
        } catch (IOException ex) {
            Logger.getLogger(ImageView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void readStegoImage(File filepath) {
        try {
            im.setHostImage(ImageIO.read(new File(filepath.getPath())));
        } catch (IOException ex) {
            Logger.getLogger(ImageView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void readCannyImage(File filepath) {
        try {
            im.setHostImage(ImageIO.read(new File(filepath.getPath())));
        } catch (IOException ex) {
            Logger.getLogger(ImageView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Image getScaledImage(int width, int height) {
        return im.getHostImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    public static void writeCiphertextLength(String cipherText) {
        int result = cipherText.length();
        try {
            FileOutputStream output = new FileOutputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\ciphertextLength.dat"));
            ObjectOutputStream o = new ObjectOutputStream(output);
            o.writeObject(result);
            o.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void writeHostImageBinary(BufferedImage img) {
        ImageIcon ic = new ImageIcon(img);
        try {
            FileOutputStream output = new FileOutputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\hostImage.dat"));
            ObjectOutputStream o = new ObjectOutputStream(output);
            o.writeObject(ic);
            o.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void writeCannyBinary(BufferedImage img) {
        ImageIcon ic = new ImageIcon(img);
        try {
            FileOutputStream output = new FileOutputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\canny.dat"));
            ObjectOutputStream o = new ObjectOutputStream(output);
            o.writeObject(ic);
            o.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void writeStegoBinary(BufferedImage img) {
        ImageIcon ic = new ImageIcon(img);
        try {
            FileOutputStream output = new FileOutputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\stego.dat"));
            ObjectOutputStream o = new ObjectOutputStream(output);
            o.writeObject(ic);
            o.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public BufferedImage toBufferImage(Image img) {
        BufferedImage imgg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bgr = imgg.createGraphics();
        bgr.drawImage(img, 0, 0, null);
        bgr.dispose();
        return imgg;
    }

    public BufferedImage readHostImageBinary() {
        BufferedImage imgg = null;
        try {
            FileInputStream input = new FileInputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\hostImage.dat"));
            ObjectInputStream in = new ObjectInputStream(input);
            ImageIcon img = (ImageIcon) in.readObject();
            imgg = toBufferImage(img.getImage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return imgg;
    }

    public BufferedImage readCannyBinary() {
        BufferedImage imgg = null;
        try {
            FileInputStream input = new FileInputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\canny.dat"));
            ObjectInputStream in = new ObjectInputStream(input);
            ImageIcon img = (ImageIcon) in.readObject();
            imgg = toBufferImage(img.getImage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return imgg;
    }

    public BufferedImage readStegoBinary() {
        BufferedImage imgg = null;
        try {
            FileInputStream input = new FileInputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\stego.dat"));
            ObjectInputStream in = new ObjectInputStream(input);
            ImageIcon img = (ImageIcon) in.readObject();
            imgg = toBufferImage(img.getImage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return imgg;
    }

    public int readCiphertextLength() {
        int result = 0;
        try {
            FileInputStream input = new FileInputStream(new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\read\\ciphertextLength.dat"));
            ObjectInputStream in = new ObjectInputStream(input);
            result = (int) in.readObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;
    }
    //Blowfish 
    public class Blowfish {

        public String key;

        public Blowfish(String key) {
            this.key = key;
        }

        public String encrypt(String plainText) {
            try {
                byte[] keyData = key.getBytes();
                SecretKeySpec secretKeySpec = new SecretKeySpec(keyData, "Blowfish");
                Cipher cipher = Cipher.getInstance("Blowfish");
                cipher.init(cipher.ENCRYPT_MODE, secretKeySpec);
                byte[] hasil = cipher.doFinal(plainText.getBytes());
                return new String(Base64.getEncoder().encode(hasil));

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String decrypt(String cipherText) {
            try {
                byte[] keyData = key.getBytes();
                SecretKeySpec secretKeySpec = new SecretKeySpec(keyData, "Blowfish");
                Cipher cipher = Cipher.getInstance("Blowfish");
                cipher.init(cipher.DECRYPT_MODE, secretKeySpec);
                byte[] hasil = cipher.doFinal(java.util.Base64.getDecoder().decode(cipherText));
                return new String(hasil);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        public void print() {
            System.out.println("berhasil");
        }
    }

    //Canny Algorithm
    public class Canny {

        private final float gaussian_cut_off = 0.005f;
        private final float magnitude_scale = 100F;
        private final float magnitude_limit = 1000F;
        private final int magnitude_max = (int) (magnitude_scale * magnitude_limit);

        private int height;
        private int width;
        private int picsize;
        private int[] data;
        private int[] magnitude;
        private BufferedImage sourceImage;
        private BufferedImage edgesImage;

        private float gaussianKernelRadius;
        private float lowThreshold;
        private float highThreshold;
        private int gaussianKernelWidth;
        private boolean contrastNormalized;

        private float[] xConv;
        private float[] yConv;
        private float[] xGradient;
        private float[] yGradient;

        public Canny() {
            lowThreshold = 2.5f;
            highThreshold = 7.5f;
            gaussianKernelRadius = 2f;
            gaussianKernelWidth = 16;
            contrastNormalized = false;
        }

        public BufferedImage getSourceImage() {
            return sourceImage;
        }

        public void setSourceImage(BufferedImage image) {
            sourceImage = image;
        }

        public BufferedImage getEdgesImage() {
            return edgesImage;
        }

        public void setEdgesImage(BufferedImage edgesImage) {
            this.edgesImage = edgesImage;

        }

        public float getLowThreshold() {
            return lowThreshold;
        }

        public void setLowThreshold(float threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException();
            }
            lowThreshold = threshold;
        }

        public float getHighThreshold() {
            return highThreshold;
        }

        public void setHighThreshold(float threshold) {
            if (threshold < 0) {
                throw new IllegalArgumentException();
            }
            highThreshold = threshold;
        }

        public int getGaussianKernelWidth() {
            return gaussianKernelWidth;

        }

        public void setGaussianKernelWidth(int gaussianKernelWidth) {
            if (gaussianKernelWidth < 2) {
                throw new IllegalArgumentException();
            }
            this.gaussianKernelWidth = gaussianKernelWidth;
        }

        public float getGaussianKernelRadius() {
            return gaussianKernelRadius;
        }

        public void setGaussianKernelRadius(float gaussianKernelRadius) {
            if (gaussianKernelRadius < 0.1f) {
                throw new IllegalArgumentException();
            }
            this.gaussianKernelRadius = gaussianKernelRadius;
        }

        public boolean isContrastNormalized() {
            return contrastNormalized;
        }

        public void setContrastNormalized(boolean contrastNormalized) {
            this.contrastNormalized = contrastNormalized;
        }

        public void process() throws IOException {
            width = sourceImage.getWidth();
            height = sourceImage.getHeight();
            picsize = width * height;
            initArrays();
            readLuminance();
            if (contrastNormalized) {
                normalizeContrast();
            }
            computeGradients(gaussianKernelRadius, gaussianKernelWidth);
            int low = Math.round(lowThreshold * magnitude_scale);
            int high = Math.round(highThreshold * magnitude_scale);
            performHysteresis(low, high);
            thresholdEdges();
            writeEdges(data);
        }

        private void initArrays() {
            if (data == null || picsize != data.length) {
                data = new int[picsize];
                magnitude = new int[picsize];
                xConv = new float[picsize];
                yConv = new float[picsize];
                xGradient = new float[picsize];
                yGradient = new float[picsize];
            }

        }

        private void computeGradients(float kernelRadius, int kernelWidth) {
            float kernel[] = new float[kernelWidth];
            float diffKernel[] = new float[kernelWidth];
            int kwidth;
            for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
                float g1 = gaussian(kwidth, kernelRadius);
                if (g1 <= gaussian_cut_off && kwidth >= 2) {
                    break;
                }
                float g2 = gaussian(kwidth - 0.5f, kernelRadius);
                float g3 = gaussian(kwidth + 0.5f, kernelRadius);
                kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
                diffKernel[kwidth] = g3 - g2;
            }

            int initX = kwidth - 1;
            int maxX = width - (kwidth - 1);
            int initY = width * (kwidth - 1);
            int maxY = width * (height - (kwidth - 1));

            //perform convolution in x and y directions
            for (int x = initX; x < maxX; x++) {
                for (int y = initY; y < maxY; y += width) {
                    int index = x + y;
                    float sumX = data[index] * kernel[0];
                    float sumY = sumX;
                    int xOffset = 1;
                    int yOffset = width;
                    for (; xOffset < kwidth;) {
                        sumY += kernel[xOffset] * (data[index - yOffset] + data[index + yOffset]);
                        sumX += kernel[xOffset] * (data[index - xOffset] + data[index + xOffset]);
                        yOffset += width;
                        xOffset++;
                    }

                    yConv[index] = sumY;
                    xConv[index] = sumX;
                }

            }

            for (int x = initX; x < maxX; x++) {
                for (int y = initY; y < maxY; y += width) {
                    float sum = 0f;
                    int index = x + y;
                    for (int i = 1; i < kwidth; i++) {
                        sum += diffKernel[i] * (yConv[index - i] - yConv[index + i]);
                    }
                    xGradient[index] = sum;
                }

            }

            for (int x = kwidth; x < width - kwidth; x++) {
                for (int y = initY; y < maxY; y += width) {
                    float sum = 0.0f;
                    int index = x + y;
                    int yOffset = width;
                    for (int i = 1; i < kwidth; i++) {
                        sum += diffKernel[i] * (xConv[index - yOffset] - xConv[index + yOffset]);
                        yOffset += width;
                    }

                    yGradient[index] = sum;
                }
            }

            initX = kwidth;
            maxX = width - kwidth;
            initY = width * kwidth;
            maxY = width * (height - kwidth);
            for (int x = initX; x < maxX; x++) {
                for (int y = initY; y < maxY; y += width) {
                    int index = x + y;
                    int indexN = index - width;
                    int indexS = index + width;
                    int indexW = index - 1;
                    int indexE = index + 1;
                    int indexNW = indexN - 1;
                    int indexNE = indexN + 1;
                    int indexSW = indexS - 1;
                    int indexSE = indexS + 1;

                    float xGrad = xGradient[index];
                    float yGrad = yGradient[index];
                    float gradMag = hypot(xGrad, yGrad);

                    //perform non-maximal supression
                    float nMag = hypot(xGradient[indexN], yGradient[indexN]);
                    float sMag = hypot(xGradient[indexS], yGradient[indexS]);
                    float wMag = hypot(xGradient[indexW], yGradient[indexW]);
                    float eMag = hypot(xGradient[indexE], yGradient[indexE]);
                    float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
                    float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
                    float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
                    float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
                    float tmp;

                    if (xGrad * yGrad <= (float) 0 /*(1)*/
                            ? Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                            ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /*(3)*/
                            && tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /*(4)*/
                            : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /*(3)*/
                            && tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /*(4)*/
                            : Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                            ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /*(3)*/
                            && tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /*(4)*/
                            : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /*(3)*/
                            && tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /*(4)*/) {
                        magnitude[index] = gradMag >= magnitude_limit ? magnitude_max : (int) (magnitude_scale * gradMag);
                    } else {
                        magnitude[index] = 0;
                    }
                }
            }
        }

        private float hypot(float x, float y) {
            return (float) Math.hypot(x, y);
        }

        private float gaussian(float x, float sigma) {
            return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
        }

        private void performHysteresis(int low, int high) {
            Arrays.fill(data, 0);

            int offset = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (data[offset] == 0 && magnitude[offset] >= high) {
                        follow(x, y, offset, low);
                    }
                    offset++;
                }
            }

        }

        private void follow(int x1, int y1, int i1, int threshold) {
            int x0 = x1 == 0 ? x1 : x1 - 1;
            int x2 = x1 == width - 1 ? x1 : x1 + 1;
            int y0 = y1 == 0 ? y1 : y1 - 1;
            int y2 = y1 == height - 1 ? y1 : y1 + 1;

            data[i1] = magnitude[i1];
            for (int x = x0; x <= x2; x++) {
                for (int y = y0; y <= y2; y++) {
                    int i2 = x + y * width;
                    if ((y != y1 || x != x1) && data[i2] == 0 && magnitude[i2] >= threshold) {
                        follow(x, y, i2, threshold);
                        return;
                    }
                }
            }

        }

        private void thresholdEdges() {
            for (int i = 0; i < picsize; i++) {
                data[i] = data[i] > 0 ? -1 : 0xff000000;
            }
        }

        private int luminance(float r, float g, float b) {
            return Math.round(0.299f * r + 0.587f * g + 0.114f * b);
        }

        private void readLuminance() {
            int type = sourceImage.getType();
            if (type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB) {
                int[] pixels = (int[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
                for (int i = 0; i < picsize; i++) {
                    int p = pixels[i];
                    int r = (p & 0xff0000) >> 16;
                    int g = (p & 0xff00) >> 8;
                    int b = p & 0xff;
                    data[i] = luminance(r, g, b);
                }
            } else if (type == BufferedImage.TYPE_BYTE_GRAY) {
                byte[] pixels = (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
                for (int i = 0; i < picsize; i++) {
                    data[i] = (pixels[i] & 0xff);
                }
            } else if (type == BufferedImage.TYPE_USHORT_GRAY) {
                short[] pixels = (short[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
                for (int i = 0; i < picsize; i++) {
                    data[i] = (pixels[i] & 0xffff) / 256;
                }
            } else if (type == BufferedImage.TYPE_3BYTE_BGR) {
                byte[] pixels = (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
                int offset = 0;
                for (int i = 0; i < picsize; i++) {
                    int b = pixels[offset++] & 0xff;
                    int g = pixels[offset++] & 0xff;
                    int r = pixels[offset++] & 0xff;
                    data[i] = luminance(r, g, b);
                }
            } else {
                throw new IllegalArgumentException("Unsupported image type: " + type);
            }

        }

        private void normalizeContrast() {
            int[] histogram = new int[256];
            for (int i = 0; i < data.length; i++) {
                histogram[data[i]]++;
            }

            int[] remap = new int[256];
            int sum = 0;
            int j = 0;
            for (int i = 0; i < histogram.length; i++) {
                sum += histogram[i];
                int target = sum * 255 / picsize;
                for (int k = j + 1; k <= target; k++) {
                    remap[k] = i;
                }
                j = target;
            }

            for (int i = 0; i < data.length; i++) {
                data[i] = remap[data[i]];
            }

        }

        private void writeEdges(int pixels[]) throws IOException {
            edgesImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            edgesImage.getWritableTile(0, 0).setDataElements(0, 0, width, height, pixels);

            try {
                ImageIO.write(edgesImage, "jpg", new File("D:\\Kuliah\\Skripsi\\Main\\Dataset\\Dataset Skripsi\\4_Canny Image.jpg"));
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(ImageView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
