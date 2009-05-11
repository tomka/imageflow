/**
 * 
 */
package imageflow.gui;

import graph.Edge;
import graph.Edges;
import graph.GList;
import graph.Node;
import graph.NodeBean;
import graph.Pin;
import graph.Selection;
import imageflow.backend.GraphController;
import imageflow.models.Connection;
import imageflow.models.Input;
import imageflow.models.Output;
import imageflow.models.SelectionList;
import imageflow.models.unit.CommentNode;
import imageflow.models.unit.UnitElement;
import imageflow.models.unit.UnitList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import visualap.Delegate;
import visualap.ErrorPrinter;
import visualap.GPanelListener;

/**
 * Graphical workspace on which the units are drawn and which handles the mouse actions.
 * @author danielsenff
 *
 */
public class GraphPanel extends JPanel implements WorkspacePanel {

	
	protected Point pick = null;
	protected Selection<Node> selection = new Selection<Node>();
	protected GList<Node> nodeL = new GList<Node>();
	protected Pin drawEdge;
	protected Edges EdgeL = new Edges();
	protected Point mouse;
	protected Rectangle rect;
	private boolean cursor;
	
	// handling of selection rectange
	protected Rectangle currentRect = null;
	protected Rectangle rectToDraw = null;
	protected Rectangle previousRectDrawn = new Rectangle();
	
	/**
	 * List of all {@link UnitElement} added to the Workflow.
	 */
	protected UnitList units;

	/**
	 * Draw a small grid on the {@link GraphPanel}
	 */
	protected boolean drawGrid = false;
	/**
	 * size of the grid
	 */
	public static int GRIDSIZE = 120; 
	/**
	 * Auto align nodes
	 */
	protected boolean align = false;

	private BufferedImage iwIcon;

	private final String iconFile = "/imageflow/resources/iw-logo.png";

	/**
	 * @param beans
	 * @param parent
	 */
	public GraphPanel(final ArrayList<Delegate> delegates, final GPanelListener parent) {

    	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		this.setBorder(BorderFactory.createLoweredBevelBorder());
		
		final DropTargetListener dropTargetListener = new DropTargetListener() {
			// Die Maus betritt die Komponente mit
			// einem Objekt
			public void dragEnter(final DropTargetDragEvent e) {}

			// Die Komponente wird verlassen 
			public void dragExit(final DropTargetEvent e) {}

			// Die Maus bewegt sich �ber die Komponente
			public void dragOver(final DropTargetDragEvent e) {}

			public void drop(final DropTargetDropEvent e) {
				try {
					final Transferable tr = e.getTransferable();
					final DataFlavor[] flavors = tr.getTransferDataFlavors();
					for (int i = 0; i < flavors.length; i++)
						if (flavors[i].isFlavorJavaFileListType()) {
							// Zun�chst annehmen
							e.acceptDrop (e.getDropAction());
							final List files = (List) tr.getTransferData(flavors[i]);
							// Wir setzen in das Label den Namen der ersten 
							// Datei
							//							label.setText(files.get(0).toString());
							e.dropComplete(true);
							return;
						}
				} catch (final Throwable t) { t.printStackTrace(); }
				// Ein Problem ist aufgetreten
				e.rejectDrop();
			}

			// Jemand hat die Art des Drops (Move, Copy, Link)
			// ge�ndert
			public void dropActionChanged(final DropTargetDragEvent e) {}

		};
		final DropTarget dropTarget = new DropTarget(this, dropTargetListener);
		this.setDropTarget(dropTarget);
		
		
		try {
//			this.iwIcon = ImageIO.read(new File(iconFile ));
			
			this.iwIcon = ImageIO.read(this.getClass().getResourceAsStream(iconFile));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * paint things that eventually go on a printer
	 * @param g
	 */
	public void paintPrintable(final Graphics g) {
		rect = new Rectangle();
		for (final Node t : nodeL) {
			rect = rect.union(t.paint(g, this));	
		}
		setPreferredSize(rect.getSize());
		Connection conn;
		for (final Edge aEdge : EdgeL) {
			conn = (Connection)aEdge; 
			final Point from = aEdge.from.getLocation();
			final Point to = aEdge.to.getLocation();
			g.setColor(  (conn.areImageBitDepthCompatible()) ? Color.BLACK : Color.RED );
			g.drawLine(from.x, from.y, to.x, to.y);
			
			if(!conn.areImageBitDepthCompatible()) {
				int dX = Math.abs(from.x - to.x)/2 + Math.min(from.x, to.x);
				int dY = Math.abs(from.y - to.y)/2 + Math.min(from.y, to.y);
				Point origin = new Point(dX, dY);
				drawErrorMessage((Graphics2D) g, "incompatible image type", origin);
			}
			
		}
		revalidate();
	}


	/* (non-Javadoc)
	 * @see visualap.GPanel#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(final Graphics g) {
		//		super.paintComponent(g);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());

		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		//paint grid
		drawGrid(g2);

		if(!nodeL.isEmpty()) {

			// paint printable items
			paintPrintable(g2);

			// paint non printable items
			if (drawEdge != null) {
				final Point origin = drawEdge.getLocation();
				//			g2.setStroke(new BasicStroke(1f));
				for (final Node node : nodeL) {
					final int margin = 15;
					// check if mouse is within this dimensions of a node
					if(isWithin2DRange(mouse, node.getOrigin(), node.getDimension(), margin)) {

						// draw every pin
						if(node instanceof UnitElement) {
							for (final Pin pin : ((UnitElement)node).getInputs()) {
								if(!drawEdge.getParent().equals(node)) 
									drawCompatbilityIndicator(g2, margin, pin);
							}

							for (final Pin pin : ((UnitElement)node).getOutputs()) {
								if(!drawEdge.getParent().equals(node))
									drawCompatbilityIndicator(g2, margin, pin);
							}	
						}

					}
				}

				g2.setColor(Color.BLACK);
				g2.drawLine(origin.x, origin.y, mouse.x, mouse.y);
				g2.draw(new Line2D.Double(origin.x, origin.y, mouse.x, mouse.y));
			}
			//If currentRect exists, paint a box on top.
			if (currentRect != null) {
				//Draw a rectangle on top of the image.
//				g2.setXORMode(Color.white); //Color of Edge varies
				//depending on image colors
				g2.setColor(new Color(0,0,255, 80));
				//			g2.setStroke(dashed);
				g2.setStroke(new BasicStroke(1f));
				g2.drawRect(rectToDraw.x, rectToDraw.y, 
						rectToDraw.width - 1, rectToDraw.height - 1);
				g2.setColor(new Color(0,0,255, 40));
				g2.fillRect(rectToDraw.x, rectToDraw.y, 
						rectToDraw.width - 1, rectToDraw.height - 1);
			}	
		} else {
			//draw a nice message to promote creating a graph

			drawWelcome(g2);
		}
	}

	/**
	 * draw a nice message to promote creating a graph
	 * @param g2
	 */
	private void drawWelcome(final Graphics2D g2) {
		final int x = 50;
		final int y = 80;

		g2.drawImage(this.iwIcon, 25, 25, null);
		
		final String headline = "Create your workflow";
		final String description = "Add new units to the graph by using the" + '\n'
			+"context menu units on this canvas." + '\n' + "   " + '\n'
			+ "A workflow is constructed from a Source-Unit and requires a Display-Unit." + '\n'
			+ "The Display-Unit is the image that will be displayed after running the workflow.";

		final Vector<String> lines = tokenizeString(description, "\n");
		
		g2.setColor(Color.GRAY);
		
		// scale font on big lengths
		final FontMetrics fm = g2.getFontMetrics();
		final int stringWidth = fm.stringWidth(headline);
		final int fontsize = 24;
		final int fontsizeOriginal = 12;
		final Font font = g2.getFont();
		final Font newFont = new Font(font.getFamily(), Font.BOLD, fontsize);
		g2.setFont(newFont);
		// and if even now to small, then cut
		g2.drawString(headline, x+5, y+15);
		g2.setFont(new Font(font.getFamily(), Font.PLAIN, fontsizeOriginal));
		int lineOffset = 45;
		for (String line : lines) {
			lineOffset +=20;
			g2.drawString(line, x+5, y+lineOffset);	
		}
	}

	/**
	 * @return the align
	 */
	public boolean isAlign() {
		return align;
	}

	/**
	 * @param align the align to set
	 */
	public void setAlign(final boolean align) {
		this.align = align;
		if(align) {
			alignElements();
		}
	}




	/** 
	 * paints a simple grid on the canvas
	 * @param g
	 */
	private void drawGrid(final Graphics g) {
		if(drawGrid) {
			g.setColor(new Color(220, 220, 220));
			for (int x = 0; x < this.getWidth(); x+=GRIDSIZE) {
				g.drawLine(x, 0, x, getHeight());
			}
			for (int y = 0; y < this.getHeight(); y+=GRIDSIZE) {
				g.drawLine(0, y, getWidth(), y);
			}	
		}
	}

	/**
	 * TODO aligns the elements to the grid
	 */
	public void alignElements() {

	}


	private void drawCompatbilityIndicator(final Graphics2D g2, final int margin, final Pin pin) {
		final int diameter = 15;
		final Point pinLocation = pin.getLocation();
		final int pinX = pinLocation.x - (diameter/2); 
		final int pinY = pinLocation.y - (diameter/2);

		// draw pin marker if mouse within inner range
		if(isWithin2DRange(mouse, pin.getLocation(), new Dimension(0,0), margin)) {

			boolean isCompatible = false;
			boolean isLoop = false;

			// we don't know, if the connection is created 
			// input first or output first
			if(drawEdge instanceof Output
					&& pin instanceof Input) {
				isLoop = ((Input)pin).isConnectedInInputBranch(drawEdge.getParent());
				isCompatible = ((Output)drawEdge).isImageBitDepthCompatible(
						((Input)pin).getImageBitDepth());
				
			} else if (drawEdge instanceof Input
					&& pin instanceof Output) {
				isLoop = ((Output)pin).existsInInputSubgraph(drawEdge.getParent());
				isCompatible = ((Input)drawEdge).isImageBitDepthCompatible(
						((Output)pin).getImageBitDepth());
			}

			g2.setColor((isCompatible && !isLoop) ? Color.green : Color.red);
			final Ellipse2D.Double circle = 
				new Ellipse2D.Double(pinX, pinY, diameter, diameter);
			g2.fill(circle);
			g2.setColor(new Color(0,0,0,44));
			g2.draw(circle);



			String errorMessage = "";
			if(!isCompatible)
				errorMessage += "Incompatible bit depth \n";
			if(isLoop) 
				errorMessage += "Loops are not allowed\n";

			if(errorMessage.length() != 0) 
				drawErrorMessage(g2, errorMessage, pin.getLocation());

		}
	}

	private void drawErrorMessage(final Graphics2D g2, String text, final Point origin) {
		
		Vector<String> lines = tokenizeString(text, "\n");
		
		g2.setColor(Color.RED);
		final Ellipse2D.Double circle = 
			new Ellipse2D.Double(origin.getX()-3, origin.getY()-3, 5, 5);
		g2.fill(circle);
		
		g2.setFont(new Font("Arial", Font.PLAIN, 12));
		final FontMetrics fm = g2.getFontMetrics();
		
		
		
		
		final Dimension dimension = new Dimension();
		String longestLine = "";
		for (String line : lines) {
			if(line.length() > longestLine.length())
				longestLine = line;
		}
		
		dimension.setSize(fm.stringWidth(longestLine) + 10, (fm.getHeight() + 4)*lines.size());
		final int padding = 2;
		
		g2.setColor(new Color(255,255,180));
		g2.fillRoundRect(origin.x-padding, origin.y-padding, 
				dimension.width+padding, dimension.height+padding, 4, 4);
		g2.setStroke(new BasicStroke(1f));
		g2.setColor(new Color(255,0,0,44));
		g2.drawRoundRect(origin.x-padding, origin.y-padding, 
				dimension.width+padding, dimension.height+padding, 4, 4);
		
		
		g2.setColor(Color.BLACK);
		
		int lineheight = fm.getHeight() + 5;
		int yT = (origin.y + padding) + fm.getAscent();
		for (String line : lines) {
			g2.drawString(line, origin.x + 5, yT);
			yT += lineheight;
		}
		
		
		
	}

	private Vector<String> tokenizeString(String text, String token) {
		StringTokenizer stringTokenizer = new StringTokenizer(text, token);
		Vector<String> lines = new Vector<String>();
		while(stringTokenizer.hasMoreTokens()) {
			lines.add(stringTokenizer.nextToken());
		}
		return lines;
	}


	/**
	 * Returns rue if the value is within this range of values.
	 * @param compareValue
	 * @param startValue
	 * @param endValue
	 * @return
	 */
	public static boolean isWithinRange(final int compareValue, 
			final int startValue, 
			final int endValue) {
		return (compareValue > startValue) 
		&& (compareValue < endValue);
	}

	/**
	 * Returns true, if the current point is within this 2D-range.
	 * The Range is defined by its coordinates, 
	 * the dimension of the rectangle and a margin around this rectangle.
	 * @param currentPoint
	 * @param origin
	 * @param dimension
	 * @param margin
	 * @return
	 */
	public static boolean isWithin2DRange(final Point currentPoint, 
			final Point origin, 
			final Dimension dimension, 
			final int margin) {
		return isWithinRange(currentPoint.x, origin.x-margin, 
				origin.x+dimension.width+margin)
				&& isWithinRange(currentPoint.y, origin.y - margin, 
						origin.y + dimension.height + margin);
	}

	/**
	 * Replace the current {@link UnitList} with a different one.
	 * @param units2
	 */
	public void setNodeL(final UnitList units2) {
		nodeL = units2;
	}

	public void properties(final Node node) {
		if (node instanceof CommentNode) {
			//			propertySheet.setVisible(false);
			final String inputValue = JOptionPane.showInputDialog("Edit text:",((CommentNode)node).getText()); 
			if ((inputValue != null)&&(inputValue.length() != 0)) {
				((CommentNode)node).setText(inputValue);
				repaint();
			}
		}
		else { // aNode instanceof NodeBean
			final UnitElement unit = (UnitElement) node;
			unit.showProperties();
		}
		//		super.properties(node);
	}


	public void setGraphController(final GraphController graphController) {
		this.selection.clear();
		this.nodeL = graphController.getUnitElements();
		this.EdgeL = graphController.getConnections();

	}

	public void setSelections(final SelectionList selections) {
		this.selection = selections;
	}

	/**
	 * Returns the selected elements on this GraphPanel
	 * @return the selection
	 */
	public Selection<Node> getSelection() {
		return this.selection;
	}
	
	/**
	 * @return the nodeL
	 */
	public GList<Node> getNodeL() {
		return this.nodeL;
	}
	
	

	public void mouseClicked(MouseEvent e) {
// generato quando il mouse viene premuto e subito rilasciato (click)
		if (e.getClickCount() > 1)
// > 1 se doppio click
//			if ((selection.size() == 1)&&(selection.get(0).edit())) repaint();
			if (selection.size() == 1) properties(selection.get(0));
			else selection.clear(); //zz to be handled in more completed way
    }

    public void mousePressed(MouseEvent e) {
    	
// generato nell'istante in cui il mouse viene premuto
		int x = e.getX();
		int y = e.getY();
// qui � obbligatorio un iteratore che scandisce la lista al contrario!
		for (ListIterator<Node> it = nodeL.listIterator(nodeL.size()); it.hasPrevious(); ) {
			Node aNode = it.previous();
			Object sel = aNode.contains(x,y);
// check selected element, is it a Node?
			if (sel instanceof Node) {
				pick = new Point(x,y);
				if (!selection.contains(aNode)) {
					selection.clear();
					selection.add(aNode);
				}
				for (Node iNode : selection)
					iNode.drag(true);
				repaint();
//				parent.showFloatingMenu(e);
				e.consume();
				changeCursor(Cursor.MOVE_CURSOR);
				return;
			}
			// check selected element, is it a Pin?
			else if (sel instanceof Pin) {
				drawEdge = (Pin) sel;
				//	System.out.println(drawEdge);
				mouse = new Point (x,y);
				changeCursor(Cursor.CROSSHAIR_CURSOR);
				return;
			} 
			// change
			/*else {
				selection.clear();
			}*/
			
			
		}
//		parent.showFloatingMenu(e);	
		
		selection.clear();
		
	//	e.consume();

	// handling of selection rectange 
		currentRect = new Rectangle(x, y, 0, 0);
		updateDrawableRect(getWidth(), getHeight());
		repaint();
	}

    public void mouseReleased(MouseEvent e) {
// generato quando il mouse viene rilasciato, anche a seguito di click
		int x = e.getX();
		int y = e.getY();
        if (pick != null) {
			for (Node iNode : selection) {
				if (cursor) iNode.translate(x-pick.x, y-pick.y);
				iNode.drag(false);
			}
			pick = null;
			repaint();
			e.consume();
			changeCursor(Cursor.DEFAULT_CURSOR);
		}
		else if (drawEdge != null)	{
			// insert new Edge if not already present in EdgeL
			for (ListIterator<Node> it = nodeL.listIterator(nodeL.size()); it.hasPrevious(); ) {
				Node aNode = it.previous();
				Object sel = aNode.contains(x,y);
				if ((sel instanceof Pin)&&(!drawEdge.equals(sel))) {
					if (!EdgeL.contains(drawEdge, (Pin) sel)) {
						EdgeL.add(drawEdge, (Pin) sel);
					}
				}
					
			}
			drawEdge = null;
			changeCursor(Cursor.DEFAULT_CURSOR);
			repaint();
		}
// handling of selection rectange
		else if (currentRect != null) {
			normaliseRect();
			for (Node aNode : nodeL)
				if (aNode.contained(currentRect)) {
					selection.add(aNode);
				}
			currentRect = null;
			repaint();
		}

//		parent.showFloatingMenu(e);
//	e.consume();
	}

    public void mouseDragged(MouseEvent e) {
// generato quando il mouse premuto viene spostato, vari eventi sono generati durante il trascinamento
		if (pick!= null) {
			for (Node iNode : selection)
				iNode.drag(e.getX()-pick.x, e.getY()-pick.y);
			repaint();
			e.consume();
		}
		else if (drawEdge != null)	{
			mouse.x = e.getX(); mouse.y = e.getY();
			repaint();
			e.consume();
		}
// handling of selection rectange
		else if (currentRect != null) updateSize(e);
	}

    public void mouseMoved(MouseEvent e) {
// generato quando il mouse viene spostato senza essere premuto
    }
    public void mouseEntered(MouseEvent e) {
// generato quando il mouse entra nella finestra
		cursor = true;
    }
    public void mouseExited(MouseEvent e) {
// generato quando il mouse esce dalla finestra
		cursor = false;
    }

    protected void changeCursor(int cursor) {
		setCursor(Cursor.getPredefinedCursor(cursor));
    }

	void updateSize(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		currentRect.setSize(x - currentRect.x, y - currentRect.y);
		updateDrawableRect(getWidth(), getHeight());
		Rectangle totalRepaint = rectToDraw.union(previousRectDrawn);
		repaint(totalRepaint.x, totalRepaint.y,
				totalRepaint.width, totalRepaint.height);
	}

	protected void updateDrawableRect(int compWidth, int compHeight) {
		int x = currentRect.x;
		int y = currentRect.y;
		int width = currentRect.width;
		int height = currentRect.height;

		//Make the width and height positive, if necessary.
		if (width < 0) {
			width = 0 - width;
			x = x - width + 1; 
			if (x < 0) {
				width += x; 
				x = 0;
			}
		}
		if (height < 0) {
			height = 0 - height;
			y = y - height + 1; 
			if (y < 0) {
				height += y; 
				y = 0;
			}
		}
		//The rectangle shouldn't extend past the drawing area.
		if ((x + width) > compWidth) {
			width = compWidth - x;
		}
		if ((y + height) > compHeight) {
			height = compHeight - y;
		}
	  
		//Update rectToDraw after saving old value.
		if (rectToDraw != null) {
			previousRectDrawn.setBounds(
						rectToDraw.x, rectToDraw.y, 
						rectToDraw.width, rectToDraw.height);
			rectToDraw.setBounds(x, y, width, height);
		} else {
			rectToDraw = new Rectangle(x, y, width, height);
		}
	}

	private void normaliseRect() {
		int x = currentRect.x;
		int y = currentRect.y;
		int width = currentRect.width;
		int height = currentRect.height;

		//Make the width and height positive, if necessary.
		if (width < 0) {
			width = - width;
			x = x - width + 1; 
			if (x < 0) {
				width += x; 
				x = 0;
			}
		}
		if (height < 0) {
			height = - height;
			y = y - height + 1; 
			if (y < 0) {
				height += y; 
				y = 0;
			}
		}
		currentRect.setBounds(x, y, width, height);
	}

	public int print(Graphics arg0, PageFormat arg1, int arg2)
			throws PrinterException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	

}