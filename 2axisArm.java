import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SCARAArmWithLabels extends JPanel {
    private final double L1 = 276.5, L2 = 164;
    private final double baseOffsetY = 119.279;
    private double theta1 = 180, theta2 = -180;
    private double targetX = 0, targetY = 0;
    private double currentX = 0, currentY = 0;
    private Timer moveTimer;
    private double stepX, stepY;
    private int stepCount;
    private List<Point> targetPoints = new ArrayList<>(); 
    private List<Point> tracePoints = new ArrayList<>(); 
    private JTextField coordField;
    private boolean isTracing = false;  
    private boolean shouldTrace = false; 

    public SCARAArmWithLabels() {
        setPreferredSize(new Dimension(500, 500));
        computeIK(currentX, currentY); // Set initial angles
        moveTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (stepCount < 50) {
                    currentX += stepX;
                    currentY += stepY;
                    computeIK(currentX, currentY);
                    stepCount++;
                    repaint();
                } else {
                    moveTimer.stop();
                }
            }
        });
    }

    public void startMove(String input) {
        try {
            String cleanedInput = input.toUpperCase().replaceAll("[^XY0-9.]", ""); // Allow decimals
            int xIndex = cleanedInput.indexOf('X');
            int yIndex = cleanedInput.indexOf('Y');
            if (xIndex == -1 || yIndex == -1 || yIndex <= xIndex) {
                System.out.println("Invalid format! Use 'X<number>Y<number>'");
                setBackground(Color.YELLOW);
                return;
            }

            double x = Double.parseDouble(cleanedInput.substring(xIndex + 1, yIndex));
            double y = Double.parseDouble(cleanedInput.substring(yIndex + 1));

            double transformedX = 230 - x;
            double transformedY = 230 - y;

            if (transformedX < 0 || transformedX > 230 || transformedY < 0 || transformedY > 230) {
                System.out.println("Target is outside the working area!");
                setBackground(Color.YELLOW);
                return;
            }
            setBackground(Color.WHITE);
            targetX = transformedX;
            targetY = transformedY;
            if (shouldTrace) {
                tracePoints.add(new Point((int) targetX, (int) targetY));
            } else {
                targetPoints.add(new Point((int) targetX, (int) targetY));
            }
            stepX = (targetX - currentX) / 50.0;
            stepY = (targetY - currentY) / 50.0;
            stepCount = 0;
            moveTimer.start();
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.out.println("Invalid input format! Use 'X<number>Y<number>'");
            setBackground(Color.YELLOW);
        }
    }

    private void computeIK(double x, double y) {
        double transformedX = x - 115;
        double transformedY = y + baseOffsetY;
        double distance = Math.sqrt(transformedX * transformedX + transformedY * transformedY);
        if (distance > (L1 + L2)) {
            System.out.println("Target out of reach!");
            return;
        }
        double cosTheta2 = (transformedX * transformedX + transformedY * transformedY - L1 * L1 - L2 * L2) / (2 * L1 * L2);
        theta2 = -Math.acos(cosTheta2);
        theta1 = Math.atan2(transformedY, transformedX) - Math.atan2(L2 * Math.sin(theta2), L1 + L2 * Math.cos(theta2));
        theta1 = Math.toDegrees(theta1);
        theta2 = Math.toDegrees(theta2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = 1.5;
        int bedWidth = (int) (230 * scale);
        int bedHeight = (int) (230 * scale);
        int bedX = getWidth() / 2 - bedWidth / 2;
        int bedY = 20;
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(bedX, bedY, bedWidth, bedHeight);

        int originX = getWidth() / 2;
        int originY = bedY + bedHeight + (int) (baseOffsetY * scale);

        int x1 = originX + (int) (L1 * scale * Math.cos(Math.toRadians(theta1)));
        int y1 = originY - (int) (L1 * scale * Math.sin(Math.toRadians(theta1)));
        int x2 = x1 + (int) (L2 * scale * Math.cos(Math.toRadians(theta1 + theta2)));
        int y2 = y1 - (int) (L2 * scale * Math.sin(Math.toRadians(theta1 + theta2)));

      
        g2.setColor(Color.MAGENTA); 
        g2.setStroke(new BasicStroke(2));
        for (int i = 1; i < tracePoints.size(); i++) {
            Point p1 = tracePoints.get(i - 1);
            Point p2 = tracePoints.get(i);
            int px1 = bedX + (int) (p1.x * scale);
            int py1 = bedY + (int) ((230 - p1.y) * scale);
            int px2 = bedX + (int) (p2.x * scale);
            int py2 = bedY + (int) ((230 - p2.y) * scale);
            g2.drawLine(px1, py1, px2, py2);
        }

        // Draw arm
        g2.setStroke(new BasicStroke(5));
        g2.setColor(Color.RED);
        g2.drawLine(originX, originY, x1, y1);
        g2.setColor(Color.BLUE);
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(Color.BLACK);
        g2.fillOval(x2 - 5, y2 - 5, 10, 10);

        // Labels
        g2.setColor(Color.BLACK);
        g2.drawString("X: " + (230 - targetX) + " Y: " + (230 - targetY), 10, 20);
        g2.drawString("Theta1: " + Math.round(theta1) + "°", 10, 40);
        g2.drawString("Theta2: " + Math.round(theta2) + "°", 10, 60);
    }

    private void processGCodeFile(File file) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toUpperCase();
                    if (line.startsWith("M3")) {
                        isTracing = true;
                        System.out.println("Starting trace...");
                    } else if (line.startsWith("M5")) {
                        isTracing = false;
                        System.out.println("Stopping trace...");
                        break;
                    } else if (isTracing && (line.startsWith("G0") || line.startsWith("G1"))) {
                        String[] parts = line.split("\\s+");
                        String xCoord = "0";
                        String yCoord = "0";
                        for (String part : parts) {
                            if (part.startsWith("X")) {
                                xCoord = part.substring(1);
                            } else if (part.startsWith("Y")) {
                                yCoord = part.substring(1);
                            }
                        }
                        String coord = "X" + xCoord + "Y" + yCoord;
                        coordField.setText(coord); 
                        if (line.startsWith("G0")) {
                            shouldTrace = false; 
                            System.out.println("Tracing OFF");
                        } else if (line.startsWith("G1")) {
                            shouldTrace = true;  
                            System.out.println("Tracing ON");
                        }
                        startMove(coord);
                        while (moveTimer.isRunning()) {
                            Thread.sleep(50);
                        }
                        Thread.sleep(100); 
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading file: " + e.getMessage());
                setBackground(Color.YELLOW);
            }
        }).start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("SCARA Arm with Labels");
        SCARAArmWithLabels panel = new SCARAArmWithLabels();
        JPanel controlPanel = new JPanel();
        panel.coordField = new JTextField("X100Y100", 10);
        JButton moveButton = new JButton("Move");
        JButton uploadButton = new JButton("Upload File");

        moveButton.addActionListener(e -> {
            String input = panel.coordField.getText();
            panel.startMove(input);
        });

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                panel.processGCodeFile(file);
            }
        });

        controlPanel.add(new JLabel("Coordinates (e.g., X10Y10):"));
        controlPanel.add(panel.coordField);
        controlPanel.add(moveButton);
        controlPanel.add(uploadButton);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}