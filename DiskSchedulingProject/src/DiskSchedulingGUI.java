import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;

public class DiskSchedulingGUI extends JFrame {

    public DiskSchedulingGUI() {
        setTitle("Disk Scheduling Simulator");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel(
                "Disk Scheduling Simulator", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 14));

        // Buttons for the three algorithms.
        JButton look = new JButton("LOOK Algorithm");
        JButton clook = new JButton("C-LOOK Algorithm");
        JButton proposed = new JButton("Proposed Algorithm");

        // Keep the simple bordered-button design.
        look.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        clook.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        proposed.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Open a separate window for each algorithm.
        look.addActionListener(e -> new DiskWindow("LOOK"));
        clook.addActionListener(e -> new DiskWindow("C-LOOK"));
        proposed.addActionListener(e -> new DiskWindow("PROPOSED"));

        // Four rows are needed for the heading and three buttons.
        JPanel menu = new JPanel(new GridLayout(4, 1, 8, 8));
        menu.setBorder(new EmptyBorder(15, 20, 15, 20));

        menu.add(title);
        menu.add(look);
        menu.add(clook);
        menu.add(proposed);

        add(menu);
        setVisible(true);
    }

    // Algorithm window: accepts input and shows the result and graph.
    class DiskWindow extends JFrame {

        JTextField count = new JTextField();
        JTextField tracks = new JTextField();
        JTextField head = new JTextField();
        JTextField requests = new JTextField();

        JComboBox<String> direction =
                new JComboBox<>(new String[]{"", "Right", "Left"});

        JLabel result = new JLabel(" ");

        Graph graph = new Graph();
        String algorithm;

        DiskWindow(String algorithm) {
            this.algorithm = algorithm;

            // Give the proposed algorithm a readable window title.
            String windowName = algorithm.equals("PROPOSED")
                    ? "Proposed Algorithm" : algorithm;
            setTitle(windowName + " Disk Scheduling");
            setSize(850, 500);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel input = new JPanel(
                    new GridLayout(6, 2, 5, 8));

            input.setBorder(new EmptyBorder(10, 10, 10, 10));

            input.add(new JLabel("Number of Requests:"));
            input.add(count);

            input.add(new JLabel("Total Tracks:"));
            input.add(tracks);

            input.add(new JLabel("Initial Head Position:"));
            input.add(head);

            input.add(new JLabel("Track Requests:"));
            input.add(requests);

            // Direction is displayed only for LOOK and C-LOOK.
            if (!algorithm.equals("PROPOSED")) {
                input.add(new JLabel("Direction:"));
                input.add(direction);
            } else {
                input.add(new JLabel(""));
                input.add(new JLabel(""));
            }

            JButton run = new JButton("Run");
            JButton reset = new JButton("Reset");

            input.add(run);
            input.add(reset);

            JPanel left = new JPanel(new BorderLayout(5, 10));
            left.setPreferredSize(new Dimension(340, 400));
            left.setBorder(new EmptyBorder(5, 5, 5, 5));

            left.add(input, BorderLayout.NORTH);
            left.add(result, BorderLayout.CENTER);

            graph.setBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            add(left, BorderLayout.WEST);
            add(graph, BorderLayout.CENTER);

            run.addActionListener(e -> calculate());
            reset.addActionListener(e -> clear());

            setVisible(true);
        }

        void calculate() {
            try {
                // The numeric fields and request list are required for every algorithm.
                if (count.getText().trim().isEmpty()
                        || tracks.getText().trim().isEmpty()
                        || head.getText().trim().isEmpty()
                        || requests.getText().trim().isEmpty()) {
                    throw new Exception("Please fill in all input fields.");
                }

                // LOOK and C-LOOK need a direction; the proposed algorithm
                // selects a path using the neighbour formula in the paper.
                if (!algorithm.equals("PROPOSED")
                        && direction.getSelectedIndex() == 0) {
                    throw new Exception("Please select a direction.");
                }

                int n = Integer.parseInt(count.getText().trim());
                int max = Integer.parseInt(tracks.getText().trim());
                int start = Integer.parseInt(head.getText().trim());

                if (n <= 0 || max < 2
                        || start < 0 || start >= max) {
                    throw new Exception(
                            "Please enter valid track and request values.");
                }

                String[] values = requests.getText().split(",");

                if (values.length != n) {
                    throw new Exception(
                            "Enter exactly " + n + " track requests.");
                }

                ArrayList<Integer> low = new ArrayList<>();
                ArrayList<Integer> high = new ArrayList<>();
                ArrayList<Integer> same = new ArrayList<>();

                for (String value : values) {
                    int t = Integer.parseInt(value.trim());

                    if (t < 0 || t >= max) {
                        throw new Exception(
                                "Track numbers must be between 0 and "
                                + (max - 1) + ".");
                    }

                    if (t < start) {
                        low.add(t);
                    } else if (t > start) {
                        high.add(t);
                    } else {
                        same.add(t);
                    }
                }

                Collections.sort(low);
                Collections.sort(high);

                ArrayList<Integer> order = new ArrayList<>();
                order.addAll(same);

                String dir = (String) direction.getSelectedItem();

                if (algorithm.equals("LOOK")) {
                    // LOOK: serve in the selected direction, then reverse.
                    if (dir.equals("Right")) {
                        order.addAll(high);
                        Collections.reverse(low);
                        order.addAll(low);
                    } else {
                        Collections.reverse(low);
                        order.addAll(low);
                        order.addAll(high);
                    }
                } else if (algorithm.equals("C-LOOK")) {
                    // C-LOOK: serve one side, then wrap around to the other side.
                    if (dir.equals("Right")) {
                        order.addAll(high);
                        order.addAll(low);
                    } else {
                        Collections.reverse(low);
                        order.addAll(low);
                        Collections.reverse(high);
                        order.addAll(high);
                    }
                } else {
                    // Proposed algorithm: apply the paper's neighbour formula.
                    order = proposedAlgorithm(low, high, same);
                }

                // Add the initial head position before the service sequence.
                ArrayList<Integer> path = new ArrayList<>();
                path.add(start);
                path.addAll(order);

                // Total head movement is the sum of distances between consecutive tracks.
                int movement = 0;

                for (int i = 1; i < path.size(); i++) {
                    movement += Math.abs(
                            path.get(i) - path.get(i - 1));
                }

                // This is average movement per request, not measured milliseconds.
                double average = (double) movement / n;

                result.setText("<html>Seek Sequence: " + path
                        + "<br><br>Total Head Movement: "
                        + movement + " tracks"
                        + "<br><br>Average Movement: "
                        + String.format("%.2f", average)
                        + " tracks/request</html>");

                graph.drawGraph(path, max);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter valid numeric values.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        /*
         * Proposed algorithm based on the research paper's stated neighbour rule.
         * LT = lowest requested track; HT = highest requested track.
         * P1 = nearest requested track below the initial head.
         * P2 = nearest requested track above the initial head.
         * A = P1 - LT; B = HT - P2.
         * If A < B, select the lower neighbour (P1); otherwise select P2.
         * The paper's written description does not unambiguously specify every
         * next-request/tie/edge case. This method is a documented interpretation:
         * after choosing a neighbour, it continues to the corresponding extreme,
         * then serves the remaining requests on the other side.
         */
        ArrayList<Integer> proposedAlgorithm(
                ArrayList<Integer> low,
                ArrayList<Integer> high,
                ArrayList<Integer> same) {

            // Requests equal to the head need no movement, so place them first.
            ArrayList<Integer> order = new ArrayList<>(same);

            // If requests exist on only one side, serve that side in order.
            if (low.isEmpty()) {
                order.addAll(high);
                return order;
            }

            if (high.isEmpty()) {
                Collections.reverse(low);
                order.addAll(low);
                return order;
            }

            // The lists are already sorted in ascending order.
            int LT = Math.min(low.get(0), high.get(0));
            int HT = Math.max(low.get(low.size() - 1),
                    high.get(high.size() - 1));

            // P1 is the nearest request below the head; P2 is nearest above it.
            int P1 = low.get(low.size() - 1);
            int P2 = high.get(0);

            // Apply the formula shown in the research paper.
            int A = P1 - LT;
            int B = HT - P2;

            if (A < B) {
                // Path 1: visit P1, continue down to LT, then serve higher requests.
                order.add(P1);
                ArrayList<Integer> remainingLow = new ArrayList<>(low);
                remainingLow.remove(Integer.valueOf(P1)); // Avoid serving P1 twice.
                Collections.reverse(remainingLow);
                order.addAll(remainingLow);
                order.addAll(high);
            } else {
                // Path 2: visit P2, continue up to HT, then serve lower requests.
                order.add(P2);
                ArrayList<Integer> remainingHigh = new ArrayList<>(high);
                remainingHigh.remove(Integer.valueOf(P2)); // Avoid serving P2 twice.
                order.addAll(remainingHigh);
                Collections.reverse(low);
                order.addAll(low);
            }

            return order;
        }

        // Clear the inputs, displayed result, and graph.
        void clear() {
            count.setText("");
            tracks.setText("");
            head.setText("");
            requests.setText("");
            direction.setSelectedIndex(0);

            result.setText(" ");
            graph.drawGraph(new ArrayList<Integer>(), 200);
        }
    }

    // Custom panel that draws the disk-head movement graph.
    class Graph extends JPanel {

        ArrayList<Integer> path = new ArrayList<>();
        int maxTracks = 200;

        void drawGraph(ArrayList<Integer> p, int max) {
            path = new ArrayList<>(p);
            maxTracks = max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Keep the graph completely blank until Run is clicked.
            if (path.isEmpty()) {
                return;
            }

            int left = 50;
            int top = 30;
            int right = getWidth() - 20;
            int bottom = getHeight() - 40;

            g.setColor(Color.GRAY);
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);

            g.drawString("Track Number", 10, 20);
            g.drawString("Request Order",
                    getWidth() / 2 - 35, getHeight() - 10);

            for (int i = 0; i <= 4; i++) {
                int y = top + i * (bottom - top) / 4;

                int track = maxTracks - 1
                        - i * (maxTracks - 1) / 4;

                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(left, y, right, y);

                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(track), 10, y + 5);
            }

            for (int i = 0; i < path.size(); i++) {
                int x = left;

                if (path.size() > 1) {
                    x = left + i * (right - left)
                            / (path.size() - 1);
                }

                int y = bottom - (int) (
                        (double) path.get(i) / (maxTracks - 1)
                        * (bottom - top));

                if (i > 0) {
                    int oldX = left + (i - 1) * (right - left)
                            / (path.size() - 1);

                    int oldY = bottom - (int) (
                            (double) path.get(i - 1)
                            / (maxTracks - 1)
                            * (bottom - top));

                    g.setColor(Color.BLUE);
                    g.drawLine(oldX, oldY, x, y);
                }

                g.setColor(Color.RED);
                g.fillOval(x - 4, y - 4, 8, 8);

                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(path.get(i)),
                        x - 10, y - 8);
            }
        }
    }

    // Main method: start the Swing interface on the event-dispatch thread.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new DiskSchedulingGUI());
    }
}
