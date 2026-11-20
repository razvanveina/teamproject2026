package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import ro.demo.usersbackend.common.Constants;

public class SimWindow extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final double K = 1.15;
	private static final int TOP = 80;
	private static final int DESIGN_W = 1560;
	private static final int DESIGN_H = 970 - TOP;
	private static final int CONV = 55;
	private static final int POINT = 55;
	private static final int TOTE = 45;
	private static final Font CONV_LABEL = new Font("Arial", Font.BOLD, 12);
	private static final Font NAME = new Font("Arial", Font.BOLD, 10).deriveFont(9.5f);

	private static final double MIN_ZOOM = 0.25;
	private static final double MAX_ZOOM = 8;

	private TimeKeeper timeKeeper = TimeKeeper.getInstance();
	private final Map<String, Entity> byName;
	private double zoom = 1;
	private double panX;
	private double panY;
	private Point last;
	private final Map<String, Shape> conveyorShapes = new HashMap<>();
	private final Map<String, Shape> pointShapes = new HashMap<>();
	private AffineTransform drawingTransform = new AffineTransform();

	public void open(Runnable action) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Simulator GUI");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(this);
			frame.pack();
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					action.run();
				}
			});
			Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			frame.setSize(Math.min(frame.getWidth(), screen.width), Math.min(frame.getHeight(), screen.height));
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	SimWindow(List<Entity> entities) {
		byName = entities.stream().collect(Collectors.toMap(Entity::getName, Function.identity()));
		setPreferredSize(new Dimension((int) (DESIGN_W * K), (int) (DESIGN_H * K)));
		setBackground(Color.WHITE);
		new Timer(50, e -> repaint()).start();

		setLayout(new FlowLayout(FlowLayout.LEFT, 16, 16));
		JButton pause = new JButton(timeKeeper.isPaused() ? "Resume" : "Pause");
		pause.setFocusPainted(false);
		pause.addActionListener(e -> {
			timeKeeper.togglePause();
			pause.setText(timeKeeper.isPaused() ? "Resume" : "Pause");
		});
		add(pause);

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				double next = Math.max(MIN_ZOOM,
						Math.min(MAX_ZOOM, zoom * Math.pow(1.1, -e.getPreciseWheelRotation())));
				double r = next / zoom;
				panX = e.getX() - (e.getX() - panX) * r;
				panY = e.getY() - (e.getY() - panY) * r;
				zoom = next;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				last = e.getPoint();
				if (e.isPopupTrigger()) {
					showAddLoadUnitMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showAddLoadUnitMenu(e);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				panX += e.getX() - last.x;
				panY += e.getY() - last.y;
				repaint();
				last = e.getPoint();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					reset();
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	private void showAddLoadUnitMenu(MouseEvent e) {
		entityAt(e.getPoint()).ifPresent(entityName -> {
			JMenuItem addLoadUnit = new JMenuItem(Constants.ADD_LOAD_UNIT);
			addLoadUnit.addActionListener(a -> showAddLoadUnitDialog(entityName));

			JPopupMenu menu = new JPopupMenu();
			menu.add(addLoadUnit);
			menu.show(this, e.getX(), e.getY());
		});
	}

	private Optional<String> entityAt(Point screenPoint) {
		Point2D drawingPoint;
		try {
			drawingPoint = drawingTransform.inverseTransform(screenPoint, null);
		} catch (NoninvertibleTransformException e) {
			return Optional.empty();
		}
		return Stream.of(pointShapes, conveyorShapes).flatMap(shapes -> shapes.entrySet().stream())
				.filter(entry -> entry.getValue().contains(drawingPoint)).map(Map.Entry::getKey).findFirst();
	}

	private void showAddLoadUnitDialog(String entityName) {
		JTextField barcodeField = new JTextField(12);
		JComboBox<String> typeBox = new JComboBox<>(new String[] { Constants.TOTE_TYPE, Constants.CARTON_TYPE });

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
		form.add(new JLabel("Barcode"));
		form.add(barcodeField);
		form.add(new JLabel("Type"));
		form.add(typeBox);

		int choice = JOptionPane.showConfirmDialog(this, form, entityName, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION) {
			return;
		}

		String barcode = barcodeField.getText().trim();
		if (barcode.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Barcode is required", Constants.ADD_LOAD_UNIT,
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Entity entity = byName.get(entityName);
		boolean isCreated = entity.addLoadUnit(new SimLoadUnit(barcode, (String) typeBox.getSelectedItem()));
		if (!isCreated) {
			String occupiedPlace = entity instanceof Conveyor ? "First slot of " + entityName : entityName;
			JOptionPane.showMessageDialog(this, occupiedPlace + " is occupied, try again later",
					Constants.ADD_LOAD_UNIT, JOptionPane.WARNING_MESSAGE);
		}
	}

	private void reset() {
		zoom = 1;
		panX = 0;
		panY = 0;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		double f = Math.min(getWidth() / (double) DESIGN_W, getHeight() / (double) DESIGN_H);
		AffineTransform screenToDrawing = new AffineTransform();
		screenToDrawing.translate(panX, panY);
		screenToDrawing.scale(zoom, zoom);
		screenToDrawing.translate((getWidth() - DESIGN_W * f) / 2, 0);
		screenToDrawing.scale(f, f);
		screenToDrawing.translate(0, -TOP);
		drawingTransform = screenToDrawing;
		g.transform(drawingTransform);

		drawConveyor(g, "C06", 498, 220, 560, 130, 560, 300);
		drawConveyor(g, "C01", 58, 440, 560, 300, 120, 300, 120, 560);
		drawConveyor(g, "C10", 325, 605, 120, 560, 560, 560);
		drawConveyor(g, "C02", 654, 524, 560, 560, 768, 560);
		drawConveyor(g, "C03", 960, 605, 768, 560, 1180, 560, 1180, 430);
		drawConveyor(g, "C15", 960, 345, 1180, 430, 1180, 300, 768, 300);
		drawConveyor(g, "C14", 1290, 392, 1440, 430, 1180, 430);
		drawConveyor(g, "C04", 654, 345, 768, 300, 560, 300);
		drawConveyor(g, "C05", 804, 220, 768, 300, 768, 130);
		drawConveyor(g, "C07", 498, 630, 560, 560, 560, 690, 664, 690);
		drawConveyor(g, "C08", 804, 630, 664, 690, 768, 690, 768, 560);
		drawConveyor(g, "C11", 58, 670, 120, 560, 120, 770);
		drawConveyor(g, "C12", 370, 734, 120, 770, 664, 770);
		drawConveyor(g, "C09", 1050, 734, 664, 770, 1480, 770);
		drawConveyor(g, "C13", 790, 945, 1480, 770, 1480, 900, 120, 900, 120, 770);

		point(g, "TPS1", 560, 130);
		point(g, "TPT1", 768, 130);
		point(g, "IP1", 560, 300);
		point(g, "DP2", 768, 300);
		point(g, "DP3", 120, 560);
		point(g, "DP1", 560, 560);
		point(g, "IP2", 768, 560);
		point(g, "EMPTY", 120, 770);
		point(g, "PTTS1", 664, 690);
		point(g, "PTTT1", 664, 770);
		point(g, "OUT", 1480, 770);
		point(g, Constants.IP3, 1180, 430);
		point(g, Constants.TPS2, 1440, 430);

		drawZone(g, Constants.PICKING_ZONE_LABEL, 620, 640, 88, 172);
		drawZone(g, Constants.DECANTING_ZONE_LABEL, 1396, 380, 88, 92);
		g.dispose();
	}

	// for picking zone rectangle
	private void drawZone(Graphics2D g, String label, int left, int top, int width, int height) {
		g.setColor(Color.DARK_GRAY);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 6f, 4f }, 0f));
		g.drawRect(left, top, width, height);
		g.setFont(CONV_LABEL);
		int labelWidth = g.getFontMetrics().stringWidth(label);
		g.drawString(label, left + (width - labelWidth) / 2, top - 6);
	}

	private void drawConveyor(Graphics2D g, String name, int labelX, int labelY, int... xy) {
		double[] centerLine = Arrays.stream(xy).asDoubleStream().toArray();
		Path2D beltPath = toPath(trimEndsToPointEdges(centerLine));
		BasicStroke beltStroke = new BasicStroke(CONV, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		conveyorShapes.put(name, beltStroke.createStrokedShape(beltPath));
		g.setColor(Color.BLACK);
		g.setStroke(beltStroke);
		g.draw(beltPath);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(CONV - 5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		g.draw(toPath(centerLine));

		g.setColor(Color.GRAY);
		g.setStroke(new BasicStroke(1.75f));

		arrow(g, xy);

		g.setColor(Color.BLACK);
		g.setFont(CONV_LABEL);
		g.drawString(name, labelX, labelY);

		Conveyor conveyor = (Conveyor) byName.get(name);
		double margin = POINT / 2.0 + TOTE / 2.0 + 2;
		double usable = length(xy) - 2 * margin;
		for (Conveyor.ToteOnBelt toteOnBelt : conveyor.getTotesOnBelt()) {
			double[] totePosition = pointAt(xy, margin + toteOnBelt.progress() * usable);
			tote(g, toteOnBelt.loadUnit(), totePosition[0], totePosition[1]);
		}
	}

	// toPath and trimEnds are both used for drawing the conveyor and points so they
	// overlap well and dont stick out
	private Path2D toPath(double[] coordinates) {
		Path2D path = new Path2D.Double();
		path.moveTo(coordinates[0], coordinates[1]);
		for (int i = 2; i < coordinates.length; i += 2) {
			path.lineTo(coordinates[i], coordinates[i + 1]);
		}
		return path;
	}

	private double[] trimEndsToPointEdges(double[] centerLine) {
		double[] trimmedCoordinates = centerLine.clone();
		moveTowardsNextPoint(trimmedCoordinates, 0, 2, POINT / 2.0);
		moveTowardsNextPoint(trimmedCoordinates, centerLine.length - 2, centerLine.length - 4, POINT / 2.0);
		return trimmedCoordinates;
	}

	private void moveTowardsNextPoint(double[] coordinates, int pointIndex, int targetIndex, double distance) {
		double deltaX = coordinates[targetIndex] - coordinates[pointIndex];
		double deltaY = coordinates[targetIndex + 1] - coordinates[pointIndex + 1];
		double segmentLength = Math.hypot(deltaX, deltaY);
		coordinates[pointIndex] += deltaX / segmentLength * distance;
		coordinates[pointIndex + 1] += deltaY / segmentLength * distance;
	}

	private double length(int[] xy) {
		double total = 0;
		for (int i = 0; i + 3 < xy.length; i += 2) {
			total += Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
		}
		return total;
	}

	private double[] pointAt(int[] xy, double d) {
		for (int i = 0; i + 3 < xy.length; i += 2) {
			double seg = Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
			if (d <= seg || i + 4 >= xy.length) {
				double t = seg == 0 ? 0 : Math.min(1, d / seg);
				return new double[] { xy[i] + t * (xy[i + 2] - xy[i]), xy[i + 1] + t * (xy[i + 3] - xy[i + 1]) };
			}
			d -= seg;
		}
		return new double[] { xy[0], xy[1] };
	}

	private void tote(Graphics2D g, SimLoadUnit loadUnit, double x, double y) {
		String barcode = loadUnit.barcode();
		int tx = (int) (x - TOTE / 2.0);
		int ty = (int) (y - TOTE / 2.0);
		g.setColor(isCarton(loadUnit) ? Constants.CARTON_COLOR : Constants.TOTE_COLOR);
		g.fillRect(tx, ty, TOTE, TOTE);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1.75f));
		g.drawRect(tx, ty, TOTE, TOTE);

		float size = NAME.getSize2D();
		g.setFont(NAME);
		while (g.getFontMetrics().stringWidth(barcode) > TOTE - 3 && size > 5f) {
			size -= 0.5f;
			g.setFont(NAME.deriveFont(size));
		}
		int w = g.getFontMetrics().stringWidth(barcode);
		g.drawString(barcode, (int) x - w / 2, (int) y + 3);
	}

	private boolean isCarton(SimLoadUnit loadUnit) {
		return Constants.CARTON_TYPE.equals(loadUnit.type());
	}

	private void point(Graphics2D g, String name, int x, int y) {
		int h = POINT / 2;
		double boxLeft = x - POINT / 2.0;
		double boxTop = y - POINT / 2.0;
		double borderWidth = (CONV - (CONV - 5)) / 2.0;
		Rectangle2D pointBox = new Rectangle2D.Double(boxLeft, boxTop, POINT, POINT);
		pointShapes.put(name, pointBox);
		g.setColor(Color.WHITE);
		g.fill(pointBox);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke((float) borderWidth));
		g.draw(new Rectangle2D.Double(boxLeft + borderWidth / 2, boxTop + borderWidth / 2, POINT - borderWidth,
				POINT - borderWidth));
		g.setFont(NAME);
		int nameWidth = g.getFontMetrics().stringWidth(name);
		if (name.equals("EMPTY") || name.equals("DP3") || name.equals(Constants.IP3)) {
			g.drawString(name, x - h - 4 - nameWidth, y + 4);
		} else if (name.equals("IP1") || name.equals("DP2")) {
			g.drawString(name, x - nameWidth / 2, y + h + 13);
		} else {
			g.drawString(name, x - nameWidth / 2, y - h - 4);
		}
		List<SimLoadUnit> lus = byName.get(name).getLoadUnits();
		if (!lus.isEmpty()) {
			tote(g, lus.get(0), x, y);
		}
	}

	private void arrow(Graphics2D g, int[] xy) {
		int best = 0;
		double bestLen = -1;
		for (int i = 0; i + 3 < xy.length; i += 2) {
			double len = Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
			if (len > bestLen) {
				bestLen = len;
				best = i;
			}
		}
		double mx = (xy[best] + xy[best + 2]) / 2.0;
		double my = (xy[best + 1] + xy[best + 3]) / 2.0;
		double a = Math.atan2(xy[best + 3] - xy[best + 1], xy[best + 2] - xy[best]);
		int len = 12;
		g.drawLine((int) mx, (int) my, (int) (mx - len * Math.cos(a - 0.6)), (int) (my - len * Math.sin(a - 0.6)));
		g.drawLine((int) mx, (int) my, (int) (mx - len * Math.cos(a + 0.6)), (int) (my - len * Math.sin(a + 0.6)));
	}
}
