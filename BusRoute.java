package busroute;

import java.util.*;

public class BusRoute {

    // Result enum for operation statuses as specified in the lab manual
    public enum Result {
        OK, NO_OP, INVALID_FIELD, INVALID_DISTANCE, DUPLICATE_ID, FULL, NOT_FOUND, BOUNDARY, EMPTY
    }

    // Stop node representing a bus stop
    public static class Stop {
        public String id;
        public String name;
        public double distanceKm;
        public Stop prev;
        public Stop next;

        public Stop(String id, String name, double distanceKm) {
            this.id = id;
            this.name = name;
            this.distanceKm = distanceKm;
            this.prev = null;
            this.next = null;
        }
    }

    // RoutePlanner class handling all doubly linked list operations and optimizations
    public static class RoutePlanner {
        private Stop head;
        private Stop tail;
        private Stop current;
        private int size;
        private static final int MAX_CAPACITY = 40;
        private HashMap<String, Stop> index;

        public RoutePlanner() {
            head = null;
            tail = null;
            current = null;
            size = 0;
            index = new HashMap<>();
        }

        public int size() {
            return size;
        }

        public Stop getCurrent() {
            return current;
        }

        public Stop find(String id) {
            return index.get(id); // O(1) average lookup using HashMap optimization
        }

        public Result addStop(String id, String name, double distanceKm) {
            if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
                return Result.INVALID_FIELD;
            }
            if (distanceKm < 0 || distanceKm > 500) {
                return Result.INVALID_DISTANCE;
            }
            if (size >= MAX_CAPACITY) {
                return Result.FULL;
            }
            if (index.containsKey(id)) {
                return Result.DUPLICATE_ID;
            }

            Stop newStop = new Stop(id, name, distanceKm);
            if (head == null) {
                head = tail = newStop;
                current = newStop; // First stop becomes current
            } else {
                tail.next = newStop;
                newStop.prev = tail;
                tail = newStop;
            }
            index.put(id, newStop);
            size++;
            return Result.OK;
        }

        public Result nextStop() {
            if (head == null) return Result.EMPTY;
            if (current == null) return Result.NOT_FOUND;
            if (current.next != null) {
                current = current.next;
                return Result.OK;
            } else {
                return Result.BOUNDARY;
            }
        }

        public Result prevStop() {
            if (head == null) return Result.EMPTY;
            if (current == null) return Result.NOT_FOUND;
            if (current.prev != null) {
                current = current.prev;
                return Result.OK;
            } else {
                return Result.BOUNDARY;
            }
        }

        private void unlink(Stop s) {
            if (s == head && s == tail) {
                head = tail = null;
            } else if (s == head) {
                head = head.next;
                if (head != null) head.prev = null;
            } else if (s == tail) {
                tail = tail.prev;
                if (tail != null) tail.next = null;
            } else {
                s.prev.next = s.next;
                s.next.prev = s.prev;
            }
            s.prev = null;
            s.next = null;
        }

        private void insertBefore(Stop m, Stop t) {
            if (t == head) {
                m.next = head;
                head.prev = m;
                head = m;
            } else {
                Stop p = t.prev;
                p.next = m;
                m.prev = p;
                m.next = t;
                t.prev = m;
            }
        }

        public Result moveBefore(String moveId, String targetId) {
            Stop m = find(moveId);
            Stop t = find(targetId);
            if (m == null || t == null) return Result.NOT_FOUND;
            if (m == t) return Result.NO_OP;
            if (m.next == t) return Result.NO_OP;

            unlink(m);
            insertBefore(m, t);
            return Result.OK;
        }

        public Result remove(String id) {
            Stop s = find(id);
            if (s == null) return Result.NOT_FOUND;

            if (s == current) {
                current = (s.next != null) ? s.next : s.prev; // null if only stop removed
            }
            unlink(s);
            index.remove(s.id);
            size--;
            return Result.OK;
        }

        public Stop search(String id) {
            return find(id);
        }

        public void displayRoute() {
            if (head == null) {
                System.out.println("The route is currently empty.");
                return;
            }
            System.out.println("\n--- Current Bus Route Order ---");
            Stop temp = head;
            int indexNum = 1;
            double prevDist = 0.0;
            while (temp != null) {
                String marker = (temp == current) ? " <== CURRENT" : "";
                String backtrackInfo = "";
                if (indexNum > 1) {
                    double leg = temp.distanceKm - prevDist;
                    backtrackInfo = " [leg " + (leg >= 0 ? "+" : "") + leg + " km]";
                    if (leg < 0) {
                        backtrackInfo += " BACKTRACK";
                    }
                }
                System.out.println(indexNum + ". ID: " + temp.id + " | Name: " + temp.name + " | Dist: " + temp.distanceKm + " km" + backtrackInfo + marker);
                prevDist = temp.distanceKm;
                temp = temp.next;
                indexNum++;
            }
            System.out.println("-------------------------------\n");
        }

        public List<Stop> distanceReport() {
            if (head == null) return new ArrayList<>();
            Stop[] copy = new Stop[size];
            int i = 0;
            for (Stop s = head; s != null; s = s.next) {
                copy[i++] = s;
            }
            // Merge sort / sorting by distance ascending, tie-break by stop ID lexicographically
            Arrays.sort(copy, (a, b) -> {
                int c = Double.compare(a.distanceKm, b.distanceKm);
                return c != 0 ? c : a.id.compareTo(b.id);
            });
            return new ArrayList<>(Arrays.asList(copy));
        }
    }

    // Main console interface application matching menu requirements
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RoutePlanner planner = new RoutePlanner();
        int choice;

        do {
            System.out.println("\n=== BUS ROUTE STOP PLANNER (OEL 07) ===");
            System.out.println("1. ADD STOP");
            System.out.println("2. NEXT STOP");
            System.out.println("3. PREVIOUS STOP");
            System.out.println("4. MOVE BEFORE");
            System.out.println("5. REMOVE STOP");
            System.out.println("6. SEARCH STOP");
            System.out.println("7. DISPLAY ROUTE");
            System.out.println("8. DISTANCE REPORT");
            System.out.println("9. EXIT");
            System.out.print("Enter your choice (1-9): ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Enter a number (1-9): ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter Stop ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Enter Stop Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Distance from Origin (0-500 km): ");
                    while (!scanner.hasNextDouble()) {
                        System.out.print("Invalid distance. Enter numeric value: ");
                        scanner.next();
                    }
                    double dist = scanner.nextDouble();
                    scanner.nextLine();
                    Result addRes = planner.addStop(id, name, dist);
                    System.out.println("Result: " + addRes);
                    break;
                case 2:
                    Result nextRes = planner.nextStop();
                    System.out.println("Result: " + nextRes);
                    if (nextRes == Result.OK && planner.getCurrent() != null) {
                        System.out.println("Current Stop: " + planner.getCurrent().id + " - " + planner.getCurrent().name);
                    }
                    break;
                case 3:
                    Result prevRes = planner.prevStop();
                    System.out.println("Result: " + prevRes);
                    if (prevRes == Result.OK && planner.getCurrent() != null) {
                        System.out.println("Current Stop: " + planner.getCurrent().id + " - " + planner.getCurrent().name);
                    }
                    break;
                case 4:
                    System.out.print("Enter Moving Stop ID: ");
                    String mId = scanner.nextLine();
                    System.out.print("Enter Target Stop ID (move before this): ");
                    String tId = scanner.nextLine();
                    Result moveRes = planner.moveBefore(mId, tId);
                    System.out.println("Result: " + moveRes);
                    break;
                case 5:
                    System.out.print("Enter Stop ID to Remove: ");
                    String rId = scanner.nextLine();
                    Result remRes = planner.remove(rId);
                    System.out.println("Result: " + remRes);
                    break;
                case 6:
                    System.out.print("Enter Stop ID to Search: ");
                    String sId = scanner.nextLine();
                    Stop found = planner.search(sId);
                    if (found != null) {
                        System.out.println("Found -> ID: " + found.id + ", Name: " + found.name + ", Distance: " + found.distanceKm + " km");
                    } else {
                        System.out.println("Result: NOT_FOUND (Stop absent)");
                    }
                    break;
                case 7:
                    planner.displayRoute();
                    break;
                case 8:
                    List<Stop> report = planner.distanceReport();
                    if (report.isEmpty()) {
                        System.out.println("The route is empty.");
                    } else {
                        System.out.println("\n--- Distance Report (Sorted Copy View) ---");
                        int idx = 1;
                        for (Stop s : report) {
                            System.out.println(idx + ". ID: " + s.id + " | Name: " + s.name + " | Dist: " + s.distanceKm + " km");
                            idx++;
                        }
                        System.out.println("-------------------------------------------\n");
                    }
                    break;
                case 9:
                    System.out.println("Exiting application. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option! Choose between 1 and 9.");
            }
        } while (choice != 9);

        scanner.close();
    }
}
