// Allocations Assistant
// Very slightly modified code from TradeMaximizer.java
// Created by Chris Okasaki (cokasaki)

import java.io.*;
import java.util.*;
import java.text.*;

public class AllocationsAssistant {
  public static void main(String[] args) { new AllocationsAssistant().run(); }
  
  final String version = "v 1.0";

  void run() {
    System.out.println("Allocations Assistant " + version);
    
    List< String[] > desiredTeams = readDesiredTeams();
    if (desiredTeams == null) return;
    System.out.println();

    buildGraph(desiredTeams);
    if (showMissing && teamNames != null && teamNames.size() > 0) {
      for (String name : usedNames) teamNames.remove(name);
      List<String> missing = new ArrayList<String>(teamNames);
      Collections.sort(missing);
      for (String name : missing) {
        System.out.println("**** Missing team named: " +name);
      }
      System.out.println();
    }
    if (showErrors && errors.size() > 0) {
      Collections.sort(errors);
      System.out.println("ERRORS:");
      for (String error : errors) System.out.println(error);
      System.out.println();
    }

    long startTime = System.currentTimeMillis();
    graph.removeImpossibleEdges();
    List<List<Graph.Vertex>> bestCycles = graph.findCycles();
    int bestSumSquares = sumOfSquares(bestCycles);
    if (iterations > 1) {
      graph.saveMatches();
      for (int i = 0; i < iterations-1; i++) {
        graph.shuffle();
        List<List<Graph.Vertex>> cycles = graph.findCycles();
        int sumSquares = sumOfSquares(cycles);
        if (sumSquares < bestSumSquares) {
          bestSumSquares = sumSquares;
          bestCycles = cycles;
          graph.saveMatches();
          int[] groups = new int[cycles.size()];
          for (int j = 0; j < cycles.size(); j++)
            groups[j] = cycles.get(j).size();
          Arrays.sort(groups);
          System.out.print("[ "+sumSquares + " :");
          for (int j = groups.length-1; j >= 0; j--)
            System.out.print(" " + groups[j]);
          System.out.println(" ]");
        }
      }
      System.out.println();
      graph.restoreMatches();
    }
    long stopTime = System.currentTimeMillis();
    displayMatches(bestCycles);

    if (showElapsedTime)
      System.out.println("Elapsed time = " + (stopTime-startTime) + "ms");
  }

  int sumOfSquares(List<List<Graph.Vertex>> cycles) {
    int sum = 0;
    for (List<Graph.Vertex> cycle : cycles) sum += cycle.size()*cycle.size();
    return sum;
  }

  boolean caseSensitive = true;
  boolean requireColons = false;
  boolean requireUsernames = false;
  boolean showErrors = true;
  boolean showRepeats = true;
  boolean showLoops = true;
  boolean showSummary = true;
  boolean showNonTrades = true;
  boolean showStats = true;
  boolean showMissing = false;
  boolean sortByItem = false;
  boolean allowDummies = false;
  boolean showElapsedTime = false;

  static final int NO_PRIORITIES = 0;
  static final int LINEAR_PRIORITIES = 1;
  static final int TRIANGLE_PRIORITIES = 2;
  static final int SQUARE_PRIORITIES = 3;
  static final int SCALED_PRIORITIES = 4;
  static final int EXPLICIT_PRIORITIES = 5;

  int priorityScheme = EXPLICIT_PRIORITIES;
  int smallStep = 1;
  int bigStep = 9;
  long nonTradeCost = 1000000000L; // 1 billion

  int iterations = 1;
  
  //////////////////////////////////////////////////////////////////////
  
  List<String> options = new ArrayList<String>();
  HashSet<String> teamNames = null;
  List<String> usedNames = new ArrayList<String>();
  
  List<String[]> readDesiredTeams() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      List<String[]> wantLists = new ArrayList<String[]>();
      boolean readingTeamNames = false;

      for (int lineNumber = 1;;lineNumber++) {
        String line = in.readLine();
        if (line == null) return wantLists;

        line = line.trim();
        if (line.length() == 0) continue; // skip blank link
        if (line.matches("#!.*")) { // declare options
          if (wantLists.size() > 0)
            fatalError("Options (#!...) cannot be declared after first team list", lineNumber);
          if (teamNames != null)
            fatalError("Options (#!...) cannot be declared after team names", lineNumber);
          for (String option : line.toUpperCase().substring(2).trim().split("\\s+")) {
            if (option.equals("CASE-SENSITIVE"))
              caseSensitive = true;
            else if (option.equals("REQUIRE-COLONS"))
              requireColons = true;
            else if (option.equals("REQUIRE-USERNAMES"))
              requireUsernames = true;
            else if (option.equals("HIDE-ERRORS"))
              showErrors = false;
            else if (option.equals("HIDE-REPEATS"))
              showRepeats = false;
            else if (option.equals("HIDE-LOOPS"))
              showLoops = false;
            else if (option.equals("HIDE-SUMMARY"))
              showSummary = false;
            else if (option.equals("HIDE-NONTRADES"))
              showNonTrades = false;
            else if (option.equals("HIDE-STATS"))
              showStats = false;
            else if (option.equals("SHOW-MISSING"))
              showMissing = true;
            else if (option.equals("SORT-BY-ITEM"))
              sortByItem = true;
            else if (option.equals("ALLOW-DUMMIES"))
              allowDummies = true;
            else if (option.equals("SHOW-ELAPSED-TIME"))
              showElapsedTime = true;
            else if (option.equals("LINEAR-PRIORITIES"))
              priorityScheme = LINEAR_PRIORITIES;
            else if (option.equals("TRIANGLE-PRIORITIES"))
              priorityScheme = TRIANGLE_PRIORITIES;
            else if (option.equals("SQUARE-PRIORITIES"))
              priorityScheme = SQUARE_PRIORITIES;
            else if (option.equals("SCALED-PRIORITIES"))
              priorityScheme = SCALED_PRIORITIES;
            else if (option.equals("EXPLICIT-PRIORITIES"))
              priorityScheme = EXPLICIT_PRIORITIES;
            else if (option.startsWith("SMALL-STEP=")) {
              String num = option.substring(11);
              if (!num.matches("\\d+"))
                fatalError("SMALL-STEP argument must be a non-negative integer",lineNumber);
              smallStep = Integer.parseInt(num);
            }
            else if (option.startsWith("BIG-STEP=")) {
              String num = option.substring(9);
              if (!num.matches("\\d+"))
                fatalError("BIG-STEP argument must be a non-negative integer",lineNumber);
              bigStep = Integer.parseInt(num);
            }
            else if (option.startsWith("NONTRADE-COST=")) {
              String num = option.substring(14);
              if (!num.matches("[1-9]\\d*"))
                fatalError("NONTRADE-COST argument must be a positive integer",lineNumber);
              nonTradeCost = Long.parseLong(num);
            }
            else if (option.startsWith("ITERATIONS=")) {
              String num = option.substring(11);
              if (!num.matches("[1-9]\\d*"))
                fatalError("ITERATIONS argument must be a positive integer",lineNumber);
              iterations = Integer.parseInt(num);
            }
            else if (option.startsWith("SEED=")) {
              String num = option.substring(5);
              if (!num.matches("[1-9]\\d*"))
                fatalError("SEED argument must be a positive integer",lineNumber);
              graph.setSeed(Long.parseLong(num));
            }
            else
              fatalError("Unknown option \""+option+"\"",lineNumber);

            options.add(option);
          }
          continue;
        }
        if (line.matches("#.*")) continue; // skip comment line
        if (line.indexOf("#") != -1) {
          if (readingTeamNames) {
            if (line.split("[:\\s]")[0].indexOf("#") != -1) {
              fatalError("# symbol cannot be used in an item name",lineNumber);
            }
          }
          else
            fatalError("Comments (#...) cannot be used after beginning of line",lineNumber);
        }

        // handle team names
        if (line.equalsIgnoreCase("!BEGIN-TEAM-NAMES")) {
          if (teamNames != null)
            fatalError("Cannot begin team names more than once", lineNumber);
          if (wantLists.size() > 0)
            fatalError("Team names cannot be declared after first real move list", lineNumber);
            
          teamNames = new HashSet<String>();
          readingTeamNames = true;
          continue;
        }
        if (line.equalsIgnoreCase("!END-TEAM-NAMES")) {
          if (!readingTeamNames)
            fatalError("!END-TEAM-NAMES without matching !BEGIN-TEAM-NAMES", lineNumber);
          readingTeamNames = false;
          continue;
        }
        if (readingTeamNames) {
          if (line.charAt(0) == ':')
            fatalError("Line cannot begin with colon",lineNumber);
          if (line.charAt(0) == '%')
            fatalError("Cannot give team names for dummy items",lineNumber);
            
          String[] toks = line.split("[:\\s]");
          String name = toks[0];
          if (!caseSensitive) name = name.toUpperCase();
          if (teamNames.contains(name))
            fatalError("Team name "+name+"+ already defined",lineNumber);
          teamNames.add(name);
          continue;
        }

        // check parens for user name
        if (line.indexOf("(") == -1 && requireUsernames)
          fatalError("Missing username with REQUIRE-USERNAMES selected",lineNumber);
        if (line.charAt(0) == '(') {
          if (line.lastIndexOf("(") > 0)
            fatalError("Cannot have more than one '(' per line",lineNumber);
          int close = line.indexOf(")");
          if (close == -1)
            fatalError("Missing ')' in username",lineNumber);
          if (close == line.length()-1)
            fatalError("Username cannot appear on a line by itself",lineNumber);
          if (line.lastIndexOf(")") > close)
            fatalError("Cannot have more than one ')' per line",lineNumber);
          if (close == 1)
            fatalError("Cannot have empty parentheses",lineNumber);

          // temporarily replace spaces in username with #'s
          if (line.indexOf(" ") < close) {
            line = line.substring(0,close+1).replaceAll(" ","#")+" "
                    + line.substring(close+1);
          }
        }
        else if (line.indexOf("(") > 0)
          fatalError("Username can only be used at the front of a want list",lineNumber);
        else if (line.indexOf(")") > 0)
          fatalError("Bad ')' on a line that does not have a '('",lineNumber);

          
        // check semicolons
        line = line.replaceAll(";"," ; ");
        int semiPos = line.indexOf(";");
        if (semiPos != -1) {
          if (semiPos < line.indexOf(":"))
            fatalError("Semicolon cannot appear before colon",lineNumber);
          String before = line.substring(0,semiPos).trim();
          if (before.length() == 0 || before.charAt(before.length()-1) == ')')
            fatalError("Semicolon cannot appear before first item on line", lineNumber);
        }
        
        // check and remove colon
        int colonPos = line.indexOf(":");
        if (colonPos != -1) {
          if (line.lastIndexOf(":") != colonPos)
            fatalError("Cannot have more that one colon on a line",lineNumber);
          String header = line.substring(0,colonPos).trim();
          if (!header.matches("(.*\\)\\s+)?[^(\\s)]\\S*"))
            fatalError("Must have exactly one item before a colon (:)",lineNumber);
          line = line.replaceFirst(":"," "); // remove colon
        }
        else if (requireColons) {
          fatalError("Missing colon with REQUIRE-COLONS selected",lineNumber);
        }

        if (!caseSensitive) line = line.toUpperCase();
        wantLists.add(line.trim().split("\\s+"));
      }
    }
    catch(Exception e) {
      fatalError(e.getMessage());
      return null;
    }
  }

  void fatalError(String msg) {
    System.out.println();
    System.out.println("FATAL ERROR: " + msg);
    System.exit(1);
  }
  void fatalError(String msg,int lineNumber) {
    fatalError(msg + " (line " + lineNumber + ")");
  }

  //////////////////////////////////////////////////////////////////////

  Graph graph = new Graph();

  List< String > errors = new ArrayList< String >();

  final long INFINITY = 100000000000000L; // 10^14
  // final long NOTRADE  = 1000000000L; // replaced by nonTradeCost
  final long UNIT     = 1L;

  int ITEMS; // the number of items being traded (including dummy items)
  int DUMMY_ITEMS; // the number of dummy items

  String[] deleteFirst(String[] a) {
    assert a.length > 0;
    String[] b = new String[a.length-1];
    for (int i = 0; i < b.length; i++) b[i] = a[i+1];
    return b;
  }
  
  void buildGraph(List< String[] > wantLists) {

    HashMap< String,Integer > unknowns = new HashMap< String,Integer >();
    
    // create the nodes
    for (int i = 0; i < wantLists.size(); i++) {
      String[] list = wantLists.get(i);
      assert list.length > 0;
      String name = list[0];
      String user = null;
      int offset = 0;
      if (name.charAt(0) == '(') {
        user = name.replaceAll("#"," "); // restore spaces in username
        // remove username from list
        list = deleteFirst(list);
          // was Arrays.copyOfRange(list,1,list.length);
          // but that caused problems on Macs
        wantLists.set(i,list);
        name = list[0];
      }
      boolean isDummy = (name.charAt(0) == '%');
      if (isDummy) {
        if (user == null)
          errors.add("**** Dummy item " + name + " declared without a username.");
        else if (!allowDummies)
          errors.add("**** Dummy items not allowed. ("+name+")");
        else {
          name += " for user " + user;
          list[0] = name;
        }
      }
      if (teamNames != null && !teamNames.contains(name) && name.charAt(0) != '%') {
        errors.add("**** Cannot define list for "+name+" because it is not a team name.");
        wantLists.set(i,null);
      }
      else if (graph.getVertex(name) != null) {
        errors.add("**** Item " + name + " has multiple want lists--ignoring all but first.  (Sometimes the result of an accidental line break in the middle of a want list.)");
        wantLists.set(i, null);
      }
      else {
        ITEMS++;
        if (isDummy) DUMMY_ITEMS++;
        Graph.Vertex vertex = graph.addVertex(name,user,isDummy);
        if (teamNames != null && teamNames.contains(name))
          usedNames.add(name);
        
        if (!isDummy) width = Math.max(width, show(vertex).length());
      }
    }

    // create the edges
    for (String[] list : wantLists) {
      if (list == null) continue; // skip the duplicate lists
      String fromName = list[0];
      Graph.Vertex fromVertex = graph.getVertex(fromName);

      // add the "no-trade" edge to itself
      graph.addEdge(fromVertex,fromVertex.twin,nonTradeCost);

      long rank = 1;
      for (int i = 1; i < list.length; i++) {
        String toName = list[i];
        if (toName.equals(";")) {
          rank += bigStep;
          continue;
        }
        if (toName.indexOf('=') >= 0) {
          if (priorityScheme != EXPLICIT_PRIORITIES) {
            errors.add("**** Cannot use '=' annotation in item "+toName+" in want list for item "+fromName+" unless using EXPLICIT_PRIORITIES.");
            continue;
          }
          if (!toName.matches("[^=]+=[0-9]+")) {
            errors.add("**** Item "+toName+" in want list for item "+fromName+" must have the format 'name=number'.");
            continue;
          }
          String[] parts = toName.split("=");
          assert(parts.length == 2);
          long explicitCost = Long.parseLong(parts[1]);
          if (explicitCost < 1) {
            errors.add("**** Explicit priority must be positive in item "+toName+" in want list for item "+fromName+".");
            continue;
          }
          rank = explicitCost;
          toName = parts[0];
        }
        if (toName.charAt(0) == '%') {
          if (fromVertex.user == null) {
            errors.add("**** Dummy item " + toName + " used in want list for item " + fromName + ", which does not have a username.");
            continue;
          }

          toName += " for user " + fromVertex.user; 
        }
        Graph.Vertex toVertex = graph.getVertex(toName);
        if (toVertex == null) {
          if (teamNames != null && teamNames.contains(toName)) {
            // this is a team without a team member 
            rank += smallStep;            
          }
          else {
            int occurrences = unknowns.containsKey(toName) ? unknowns.get(toName) : 0;
            unknowns.put(toName,occurrences + 1);
          }
          continue;
        }
        
        toVertex = toVertex.twin; // adjust to the sending vertex
        if (toVertex == fromVertex.twin) {
          errors.add("**** Item " + toName + " appears in its own want list.");
        }
        else if (graph.getEdge(fromVertex,toVertex) != null) {
          if (showRepeats)
            errors.add("**** Item " + toName + " is repeated in want list for " + fromName + ".");
        }
        else if (!toVertex.isDummy &&
                 fromVertex.user != null &&
                 fromVertex.user.equals(toVertex.user)) {
          errors.add("**** Item "+fromVertex.name +" contains item "+toVertex.name+" from the same user ("+fromVertex.user+")");
        }
        else {
          long cost = UNIT;
          switch (priorityScheme) {
            case LINEAR_PRIORITIES:   cost = rank; break;
            case TRIANGLE_PRIORITIES: cost = rank*(rank+1)/2; break;
            case SQUARE_PRIORITIES:   cost = rank*rank; break;
            case SCALED_PRIORITIES:   cost = rank; break; // assign later
            case EXPLICIT_PRIORITIES: cost = rank; break;
          }

          // all edges out of a dummy node have the same cost
          if (fromVertex.isDummy) cost = nonTradeCost;

          graph.addEdge(fromVertex,toVertex,cost);

          rank += smallStep;
        }
      }

      // update costs for those priority schemes that need information such as
      // number of wants
      if (!fromVertex.isDummy) {
        switch (priorityScheme) {
          case SCALED_PRIORITIES:
            int n = fromVertex.edges.size()-1;
            for (Graph.Edge edge : fromVertex.edges) {
              if (edge.sender != fromVertex.twin)
                edge.cost = 1 + (edge.cost-1)*2520/n;
            }
            break;
        }
      }
    }

    graph.freeze();

    for (Map.Entry< String,Integer > entry : unknowns.entrySet()) {
      String item = entry.getKey();
      int occurrences = entry.getValue();
      String plural = occurrences == 1 ? "" : "s";
      errors.add("**** Unknown item " + item + " (" + occurrences + " occurrence" + plural + ")");
    }

  } // end buildGraph

  String show(Graph.Vertex vertex) {
    if (vertex.user == null || vertex.isDummy) return vertex.name;
    else if (sortByItem) return vertex.name + " " + vertex.user;
    else return vertex.user + " " + vertex.name;
  }

  String showuser(Graph.Vertex vertex) {
    if (vertex.user == null || vertex.isDummy) return vertex.name;
    else if (sortByItem) return vertex.user;
    else return vertex.user;
  }

  String showname(Graph.Vertex vertex) {
    if (vertex.user == null || vertex.isDummy) return vertex.name;
    else if (sortByItem) return vertex.name;
    else return vertex.name;
  }



  //////////////////////////////////////////////////////////////////////
  
  void displayMatches(List<List<Graph.Vertex>> cycles) {
    int numTrades = 0;
    int numGroups = cycles.size();
    int totalCost = 0;
    int sumOfSquares = 0;
    List< Integer > groupSizes = new ArrayList< Integer >();

    List< String > summary = new ArrayList< String >();
    List< String > loops = new ArrayList< String >();

    for (List<Graph.Vertex> cycle : cycles) {
      int size = cycle.size();
      numTrades += size;
      sumOfSquares += size*size;
      groupSizes.add(size);
      for (Graph.Vertex v : cycle) {
        assert v.match != v.twin;
        loops.add((showuser(v)) + " moves to " + showname(v.match.twin));
//        summary.add((showuser(v)) + " moves to " + pad(showname(v.match.twin)) + " and sends to " + show(v.twin.match));
        totalCost += v.matchCost;
      }
      loops.add("");
    }
    if (showNonTrades) {
      for (Graph.Vertex v : graph.RECEIVERS) {
        if (v.match == v.twin && !v.isDummy)
          loops.add((showuser(v)) + " stays on " + (showname(v)));
      }
      for (Graph.Vertex v : graph.orphans) {
        if (!v.isDummy)
          loops.add((showuser(v)) + " stays on " + (showname(v)));
      }
    }

    if (showLoops) {
      System.out.println("Rotations (" + numTrades + " total moves):");
      System.out.println();
      for (String item : loops) System.out.println(item);
    }
    
    if (showSummary) {
     Collections.sort(summary);
//      System.out.println("ITEM SUMMARY (" + numTrades + " total trades):");
      System.out.println();
      for (String item : summary) System.out.println(item);
      System.out.println();
    }

    
    System.out.print("Num trades  = " + numTrades + " of " + (ITEMS-DUMMY_ITEMS) + " items");    
    if (ITEMS-DUMMY_ITEMS == 0) System.out.println();
    else System.out.println(new DecimalFormat(" (0.0%)").format(numTrades/(double)(ITEMS-DUMMY_ITEMS)));
    
    if (showStats) {
      System.out.print("Total cost  = " + totalCost);
      if (numTrades == 0) System.out.println();
      else System.out.println(new DecimalFormat(" (avg 0.00)").format(totalCost/(double)numTrades));
      System.out.println("Num groups  = " + numGroups);
//      System.out.print("Group sizes =");
      Collections.sort(groupSizes);
      Collections.reverse(groupSizes);
//      for (int groupSize : groupSizes) System.out.print(" " + groupSize);
//      System.out.println();
//      System.out.println("Sum squares = " + sumOfSquares);

//      System.out.println("Orphans     = " + graph.orphans.size());
    }
  }

  int width = 1;
  String pad(String name) {
    while (name.length() < width) name += " ";
    return name;
  }

} // end AllocationsAssistant
