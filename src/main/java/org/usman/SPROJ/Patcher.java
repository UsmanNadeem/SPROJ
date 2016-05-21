package org.usman.SPROJ;
import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Usman on 06-May-16.
 */
public class Patcher {

    private static ArrayList<String> readFileandFix(File file, String methodName, String methodReturnType,
                                                    String sourceLine, String sourceType, String registerNum, int methodParamNum,
                                                    int registerCount) {
        // file
        // startwith .method and contains nextline and nextline

        // endswith nextline type

        // shoudl not contain .method again

        // com/example/irtazasafi/mnemorizer/GPSTracker.smali
        // getLocation
        // 1
        // 8
        // Landroid/location/Location;
        // , Landroid/location/Location;->getLatitude()
        // D
        // 0
        BufferedReader br = null;
        ArrayList<String> output = new ArrayList<>();
        boolean inTargetFunction = false;
        boolean sourceScopeStarted = false;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            int latestTotalNumRegisters = 0;
            while ((line = br.readLine()) != null) {
                if (!inTargetFunction && line.startsWith(".method") && line.contains(methodName) && line.contains(methodReturnType)) {
                    inTargetFunction = true;
                }
                if (line.startsWith(".end method")) {
                    inTargetFunction = false;
                }
//                if (line.startsWith("    .locals ") && inTargetFunction) {
//                    Pattern pattern = Pattern.compile("[0-9]+");
//                    Matcher matcher = pattern.matcher(line);
//
//                    matcher.find();
//                    latestTotalNumRegisters = Integer.parseInt(matcher.group())+1;
//                    line = "    .locals " + (Integer.parseInt(matcher.group())+1);  // increase locals by one
//                }
                if (line.contains(sourceLine) && line.endsWith(sourceType) && inTargetFunction) {
                    output.add(line);
//                    sourceScopeStarted = true;
                    line = br.readLine(); // blank line
                    output.add(line);
                    line = br.readLine(); // move-result most probably
                    if (!line.endsWith("v"+registerNum)) {
                        // to check if it is the same source that leads to the sink and not a similar source that doesnot
                        output.add(line);
                        continue;
                    }

                    int numIndentation = line.indexOf("move-res");
                    if (numIndentation == -1) numIndentation = 0;
                    String indentation = "";
                    for (int ii = 0; ii < numIndentation; ++ii) {
                        indentation += " ";
                    }

                    if (sourceType.contains("Ljava/lang/String")){
                        line = "\n"+indentation+"const-string v"+registerNum+", \"usman\"\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"myString\":Ljava/lang/String;\n";
                        output.add(line);
                    } else if (sourceType.equals("I")){
                        line = "\n"+indentation+"const/16 v"+registerNum+", 0x37\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"myint\":I\n";
                        output.add(line);
                    } else if (sourceType.equals("F")){
                        line = "\n"+indentation+"const v"+registerNum+", 0x440ac000    # 555.0f\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"myfloat\":F\n";
                        output.add(line);
                    } else if (sourceType.equals("Z")){
                        line = "\n"+indentation+"const/4 v"+registerNum+", 0x1\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"myboolean\":Z\n";
                        output.add(line);
                    } else if (sourceType.equals("B")){
                        line = "\n"+indentation+"const/4 v"+registerNum+", 0x5\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"mybyte\":B\n";
                        output.add(line);
                    } else if (sourceType.equals("S")){
                        line = "\n"+indentation+"const/16 v"+registerNum+", 0x37\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"myshort\":S\n";
                        output.add(line);
                    } else if (sourceType.equals("C")){
                        line = "\n"+indentation+"const/16 v"+registerNum+", 0x61\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"mychar\":C\n";
                        output.add(line);
                    } else if (sourceType.equals("J")){
                        line = "\n"+indentation+"const-wide/16 v"+registerNum+", 0x22b\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"mylong\":J\n";
                        output.add(line);
                    } else if (sourceType.equals("D")){
                        line = "\n"+indentation+"const-wide v"+registerNum+", 0x4081580000000000L    # 555.0\n";
                        output.add(line);
                        line = indentation+".local v"+registerNum+", \"mydouble\":D\n";
                        output.add(line);
                    }
                    continue;
                }
//                if (line.contains(".end local v"+registerNum)) {
//                    sourceScopeStarted = false;
//                }
//                if (line.contains("move-res") && line.contains("v"+registerNum)) {
//                    sourceScopeStarted = false;
//                }
                // replace everything
//                if (sourceScopeStarted && inTargetFunction) {
//                    String newRegNum = "v"+(latestTotalNumRegisters-1);
//                    String oldRegNum = "v"+registerNum;
//                    if (line.contains(oldRegNum)) {
//                        line = line.replaceAll(oldRegNum, newRegNum);
//                    }
//                }
                output.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return output;
    }

    private static void updateFile(File toWrite, ArrayList<String> output) {
        try {
            BufferedWriter myWriter = new BufferedWriter(new FileWriter(toWrite.getAbsoluteFile()));
            for (String str : output) {
                myWriter.write(str + "\n");
            }

            myWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        BufferedReader br = null;
        File sourceFile = new File("input.txt");
        ///////
        try {
            br = new BufferedReader(new FileReader(sourceFile));
            String line;
            String fileToModify;
            while ((line = br.readLine()) != null) {
                fileToModify = line;
                File toRead = new File(args[0]+"/smali/"+line);
                String methodName = br.readLine();
                String methodReturnType = br.readLine();
                int methodParamNum = Integer.parseInt(br.readLine());
                int registerCount = Integer.parseInt(br.readLine());
                String sourceLine = br.readLine();
                String sourceType = br.readLine();
                String registerNum = br.readLine();
                if (sourceType.contains("Ljava/lang/String")||sourceType.equals("I")
                        ||sourceType.equals("I")||sourceType.equals("F")||sourceType.equals("Z")
                        ||sourceType.equals("B")||sourceType.equals("S")||sourceType.equals("C")
                        ||sourceType.equals("J")||sourceType.equals("D")) {

                    ArrayList<String> output = readFileandFix(toRead, methodName, methodReturnType,
                            sourceLine, sourceType, registerNum, methodParamNum, registerCount);

                    // clear file
                    PrintWriter writer = new PrintWriter(toRead);
                    writer.close();
                    updateFile(toRead, output);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}