/**
 * 
 */
package test;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import models.UnitFactoryExt;

import de.danielsenff.imageflow.models.unit.NodeIcon;
import de.danielsenff.imageflow.models.unit.UnitElement;




/**
 * @author danielsenff
 *
 */
public class IconPreview extends JFrame {

	private BufferedImage bi;
	private NodeIcon icon;



	/**
	 * 
	 */
	public IconPreview() {
		UnitElement createGaussianBlurUnit = UnitFactoryExt.createGaussianBlurUnit();
		createGaussianBlurUnit.setDisplay(true);
		icon = new NodeIcon(createGaussianBlurUnit);
		
		setTitle("Icon Test");
		setSize(400,200);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
	}
	
	@Override
	public void paint(Graphics g) {
		
		Graphics2D g2 = (Graphics2D)g; 
//		bi = icon.getImage();
		int width =  this.getWidth(), height =  this.getHeight();

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height+25);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   
//        g.drawImage(bi, 0, 25, null);
		icon.paintBigIcon(g2);
		
//		icon.setSelected(true);
		
		
		g2.translate(150, 0);
		icon.paintMediumIcon(g2);
		
		g2.translate(150, 0);
		icon.paintSmallIcon(g2);
	}
	
	public static void main(String[] args) {
		new IconPreview();
	}
	
}
