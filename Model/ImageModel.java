/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 *
 * @author user
 */
public class ImageModel {

    BufferedImage hostImage;
    BufferedImage stegoImage;
    BufferedImage cannyImage;

    double PSNR;
    double MSE;

    public ImageModel() {
        this.hostImage = null;
        this.stegoImage = null;
        this.stegoImage = null;
        this.PSNR = 0;
        this.MSE = 0;
    }

    public void setHostImage(BufferedImage hostImage) {
        this.hostImage = hostImage;
    }

    public BufferedImage getHostImage() {
        return this.hostImage;
    }

    public void setStegoImage(BufferedImage stegoImage) {
        this.stegoImage = stegoImage;
    }

    public BufferedImage getStegoImage() {
        return this.stegoImage;
    }

    public void setCannyImage(BufferedImage cannyImage) {
        this.cannyImage = cannyImage;
    }

    public BufferedImage getCannyImage() {
        return this.cannyImage;
    }

    public void setPSNR(double PSNR) {
        this.PSNR = PSNR;
    }

    public double getPSNR() {
        return this.PSNR;
    }

    public void setMSE(double MSE) {
        this.MSE = MSE;
    }

    public double getMSE() {
        return this.MSE;
    }

}
