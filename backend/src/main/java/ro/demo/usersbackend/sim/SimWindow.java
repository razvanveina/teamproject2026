package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import ro.demo.usersbackend.common.Constants;

public class SimWindow extends JPanel {

	public static final int CONV = 55;
	public static final int POINT = 55;
	public static final int TOTE = 45;
	public static final Font CONV_LABEL = new Font("Arial", Font.BOLD, 12);
	public static final Font NAME = new Font("Arial", Font.BOLD, 10).deriveFont(9.5f);

	private static final long serialVersionUID = 1L;
	private static final double K = 1.15;
	private static final int TOP = 80;
	private static final int DESIGN_W = 1760;
	private static final int DESIGN_H = 1300 - TOP;
	private static final double MIN_ZOOM = 0.25;
	private static final double MAX_ZOOM = 8;

	private Sim sim;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();
	private double zoom = 1;
	private double panX;
	private double panY;
	private Point last;
	private final Map<String, Entity> byName;
	private final Map<String, Shape> conveyorShapes = new HashMap<>();
	private final Map<String, Shape> pointShapes = new HashMap<>();
	private AffineTransform drawingTransform = new AffineTransform();
	private Conveyor draggedConveyor;
	private int draggedAnchorIndex;
	private Point dragStart;
	private Point anchorStart;
	private boolean isExtendingConveyor;
	private boolean hasMovedAnchor;
	private ServerPoint draggedPoint;
	private final List<AttachedAnchor> attachedAnchors = new ArrayList<>();

	private record AttachedAnchor(Conveyor conveyor, int anchorIndex) {
	}

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

	SimWindow(Sim sim) {
		this.sim = sim;
		byName = sim.getEntities().stream().collect(Collectors.toMap(Entity::getName, Function.identity()));

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

		JComboBox<Double> conveyorSpeedBox = new JComboBox<>(Constants.CONVEYOR_SPEED_FACTORS);
		conveyorSpeedBox.setSelectedItem(1.0);
		conveyorSpeedBox.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object speedFactor, int index,
					boolean isSelected, boolean hasFocus) {
				return super.getListCellRendererComponent(list, speedFactor + "x", index, isSelected, hasFocus);
			}
		});
		conveyorSpeedBox.addActionListener(e -> applyConveyorSpeedFactor((Double) conveyorSpeedBox.getSelectedItem()));
		add(new JLabel("Conveyor speed"));
		add(conveyorSpeedBox);

		JButton resetButton = new JButton("Reset");
		resetButton.setFocusPainted(false);
		resetButton.addActionListener(e -> {
			int answer = JOptionPane.showConfirmDialog(this, "Remove all load units from the simulator?", "Reset",
					JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				new Thread(sim::clearLoadUnits, "simReset").start();
			}
		});
		add(resetButton);

		JButton setupButton = new JButton("Setup");
		setupButton.setFocusPainted(false);
		setupButton.addActionListener(e -> {
			int answer = JOptionPane.showConfirmDialog(this,
					"Remove all load units and load the ones the server has on conveyor points?", "Setup",
					JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				new Thread(sim::setupFromServer, "simSetup").start();
			}
		});
		add(setupButton);

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
				if (isDeleteAnchorClick(e)) {
					deleteAnchor(e.getPoint());
					return;
				}
				if (e.isPopupTrigger()) {
					showAddLoadUnitMenu(e);
				}
				if (SwingUtilities.isLeftMouseButton(e) && timeKeeper.isPaused()) {
					boolean isExtending = e.isControlDown();
					boolean isMovingStackedTogether = e.isShiftDown();
					grabAnchor(e.getPoint(), isExtending, isMovingStackedTogether);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger() && !isDeleteAnchorClick(e)) {
					showAddLoadUnitMenu(e);
				}
				if (hasMovedAnchor) {
					recalculateEditedConveyors();
				}
				hasMovedAnchor = false;
				draggedConveyor = null;
				isExtendingConveyor = false;
				draggedPoint = null;
				attachedAnchors.clear();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (draggedConveyor != null) {
					dragAnchor(e.getPoint());
				} else {
					panX += e.getX() - last.x;
					panY += e.getY() - last.y;
				}
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

	private void applyConveyorSpeedFactor(double speedFactor) {
		for (Entity entity : sim.getEntities()) {
			if (entity instanceof Conveyor conveyor) {
				conveyor.setSpeedFactor(speedFactor);
			}
		}
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

	private void grabAnchor(Point screenPoint, boolean isExtending, boolean isMovingStackedTogether) {
		Point drawingPoint = toDrawingPoint(screenPoint);
		if (drawingPoint == null) {
			return;
		}
		for (Entity entity : sim.getEntities()) {
			if (entity instanceof Conveyor conveyor) {
				int anchorIndex = conveyor.anchorIndexAt(drawingPoint);
				boolean isAnchorHit = anchorIndex != Constants.NO_ANCHOR;
				boolean canGrab = isAnchorHit && (!isExtending || conveyor.isLastAnchor(anchorIndex));
				if (canGrab) {
					draggedConveyor = conveyor;
					draggedAnchorIndex = anchorIndex;
					dragStart = drawingPoint;
					anchorStart = conveyor.getAnchorPosition(anchorIndex);
					isExtendingConveyor = isExtending;
					if (isMovingStackedTogether) {
						draggedPoint = serverPointAt(anchorStart);
						collectAnchorsAttachedTo(draggedPoint);
					} else if (conveyor.isFirstAnchor(anchorIndex)) {
						draggedPoint = pointFeeding(conveyor, anchorStart);
					}
					return;
				}
			}
		}
	}

	private void dragAnchor(Point screenPoint) {
		Point drawingPoint = toDrawingPoint(screenPoint);
		if (drawingPoint == null) {
			return;
		}
		int distanceX = drawingPoint.x - dragStart.x;
		int distanceY = drawingPoint.y - dragStart.y;
		boolean isHorizontalDrag = Math.abs(distanceX) >= Math.abs(distanceY);

		Point newAnchorPosition;
		if (isHorizontalDrag) {
			newAnchorPosition = new Point(anchorStart.x + distanceX, anchorStart.y);
		} else {
			newAnchorPosition = new Point(anchorStart.x, anchorStart.y + distanceY);
		}

		if (isExtendingConveyor) {
			if (newAnchorPosition.equals(anchorStart)) {
				return;
			}
			draggedAnchorIndex = draggedConveyor.addAnchorAtEnd();
			isExtendingConveyor = false;
		}
		draggedConveyor.moveAnchor(draggedAnchorIndex, newAnchorPosition);
		hasMovedAnchor = true;
		if (draggedPoint != null) {
			draggedPoint.moveTo(newAnchorPosition);
		}
		for (AttachedAnchor attachedAnchor : attachedAnchors) {
			attachedAnchor.conveyor().moveAnchor(attachedAnchor.anchorIndex(), newAnchorPosition);
		}
	}

	private void recalculateEditedConveyors() {
		draggedConveyor.recalculateCapacity();
		for (AttachedAnchor attachedAnchor : attachedAnchors) {
			attachedAnchor.conveyor().recalculateCapacity();
		}
	}

	private ServerPoint pointFeeding(Conveyor conveyor, Point position) {
		ServerPoint serverPoint = serverPointAt(position);
		boolean isFeedingConveyor = serverPoint != null && conveyor.getSource().contains(serverPoint);
		if (isFeedingConveyor) {
			return serverPoint;
		}
		return null;
	}

	private ServerPoint serverPointAt(Point position) {
		for (Entity entity : sim.getEntities()) {
			if (entity instanceof ServerPoint serverPoint && serverPoint.isAt(position)) {
				return serverPoint;
			}
		}
		return null;
	}

	private void collectAnchorsAttachedTo(ServerPoint serverPoint) {
		attachedAnchors.clear();
		if (serverPoint == null) {
			return;
		}
		int firstAnchorIndex = 0;
		for (Entity entity : sim.getEntities()) {
			if (entity instanceof Conveyor conveyor && conveyor != draggedConveyor) {
				boolean startsAtPoint = conveyor.getSource().contains(serverPoint);
				boolean endsAtPoint = serverPoint.getSource().contains(conveyor);
				if (startsAtPoint) {
					attachedAnchors.add(new AttachedAnchor(conveyor, firstAnchorIndex));
				}
				if (endsAtPoint) {
					attachedAnchors.add(new AttachedAnchor(conveyor, conveyor.getLastAnchorIndex()));
				}
			}
		}
	}

	private boolean isDeleteAnchorClick(MouseEvent e) {
		return SwingUtilities.isRightMouseButton(e) && e.isControlDown() && timeKeeper.isPaused();
	}

	private void deleteAnchor(Point screenPoint) {
		Point drawingPoint = toDrawingPoint(screenPoint);
		if (drawingPoint == null) {
			return;
		}
		for (Entity entity : sim.getEntities()) {
			if (entity instanceof Conveyor conveyor) {
				int anchorIndex = conveyor.anchorIndexAt(drawingPoint);
				boolean isAnchorHit = anchorIndex != Constants.NO_ANCHOR;
				boolean isLastAnchorHit = isAnchorHit && conveyor.isLastAnchor(anchorIndex);
				if (isLastAnchorHit && conveyor.canRemoveAnchor()) {
					conveyor.removeLastAnchor();
					conveyor.recalculateCapacity();
					repaint();
					return;
				}
			}
		}
	}

	private Point toDrawingPoint(Point screenPoint) {
		Point drawingPoint = new Point();
		try {
			drawingTransform.inverseTransform(screenPoint, drawingPoint);
		} catch (NoninvertibleTransformException e) {
			return null;
		}
		return drawingPoint;
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
		boolean isCreated = entity.addLoadUnit(new SimLoadUnit(barcode, (String) typeBox.getSelectedItem()), sim);
		if (!isCreated) {
			String occupiedPlace = entity instanceof Conveyor ? "First slot of " + entityName : entityName;
			JOptionPane.showMessageDialog(this,
					occupiedPlace + " is occupied or the tote already exists, check logs and try again later",
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

		for (Entity entity : sim.getEntities()) {
			if (entity instanceof Conveyor conveyor) {
				conveyorShapes.put(conveyor.getName(), conveyor.draw(g));
			}
		}

		for (Entity entity : sim.getEntities()) {
			if (entity instanceof ServerPoint serverPoint) {
				pointShapes.put(serverPoint.getName(), serverPoint.draw(g));
			}
		}

		if (timeKeeper.isPaused()) {
			for (Entity entity : sim.getEntities()) {
				if (entity instanceof Conveyor conveyor) {
					conveyor.drawAnchors(g);
				}
			}
		}

		drawZone(g, Constants.PICKING_ZONE_LABEL + " 1", 620, 640, 88, 172);
		drawZone(g, Constants.PICKING_ZONE_LABEL + " 2", 940, 640, 88, 172);
		drawZone(g, Constants.PICKING_ZONE_LABEL + " 3", 1260, 640, 88, 172);
		drawZone(g, Constants.DECANTING_ZONE_LABEL, 1616, 380, 88, 92);
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

}
