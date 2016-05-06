package org.usman.SPROJ;
import java.io.*;
import java.util.ArrayList;

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
            while ((line = br.readLine()) != null) {
                if (!inTargetFunction && line.startsWith(".method") && line.contains(methodName) && line.contains(methodReturnType)) {
                    inTargetFunction = true;
                } else if (line.startsWith(".end method")) {
                    inTargetFunction = false;
                } else if (line.startsWith("    .locals ") && inTargetFunction) {
                    line = "    .locals " + (registerCount-methodParamNum+1);  // increase locals by one
                } else if (line.contains(sourceLine) && line.endsWith(sourceType)) {
                    output.add(line);
                    sourceScopeStarted = true;
                    line = br.readLine(); // blank line
                    output.add(line);
                    line = br.readLine(); // move-result most probably
                    output.add(line);

                    int numIndentation = line.indexOf("move-res");
                    if (numIndentation == -1) numIndentation = 0;
                    String indentation = "";
                    for (int ii = 0; ii < numIndentation; ++ii) {
                        indentation += " ";
                    }

                    if (sourceType.contains("Ljava/lang/String")){
                        line = indentation+"const-string v"+(registerCount-methodParamNum)+", \"usman\"";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"myString\":Ljava/lang/String;";
                        output.add(line);
                    } else if (sourceType.equals("I")){
                        line = indentation+"const/16 v"+(registerCount-methodParamNum)+", 0x37";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"myint\":I";
                        output.add(line);
                    } else if (sourceType.equals("F")){
                        line = indentation+"const v"+(registerCount-methodParamNum)+", 0x440ac000    # 555.0f";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"myfloat\":F";
                        output.add(line);
                    } else if (sourceType.equals("Z")){
                        line = indentation+"const/4 v"+(registerCount-methodParamNum)+", 0x1";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"myboolean\":Z";
                        output.add(line);
                    } else if (sourceType.equals("B")){
                        line = indentation+"const/4 v"+(registerCount-methodParamNum)+", 0x5";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"mybyte\":B";
                        output.add(line);
                    } else if (sourceType.equals("S")){
                        line = indentation+"const/16 v"+(registerCount-methodParamNum)+", 0x37";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"myshort\":S";
                        output.add(line);
                    } else if (sourceType.equals("C")){
                        line = indentation+"const/16 v"+(registerCount-methodParamNum)+", 0x61";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"mychar\":C";
                        output.add(line);
                    } else if (sourceType.equals("J")){
                        line = indentation+"const-wide/16 v"+(registerCount-methodParamNum)+", 0x22b";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"mylong\":J";
                        output.add(line);
                    } else if (sourceType.equals("D")){
                        line = indentation+"const-wide v"+(registerCount-methodParamNum)+", 0x4081580000000000L    # 555.0";
                        output.add(line);
                        line = indentation+".local v"+(registerCount-methodParamNum)+", \"mydouble\":D";
                        output.add(line);
                    }
                    continue;
                } else if (line.contains(".end local v"+registerNum)) {
                    sourceScopeStarted = false;
                }
                // replace everything
                if (sourceScopeStarted && inTargetFunction) {
                    String newRegNum = "v"+(registerCount-methodParamNum);
                    String oldRegNum = "v"+registerNum;
                    if (line.contains(oldRegNum)) {
                        line.replaceAll(oldRegNum, newRegNum);
                    }
                }
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
        try {
            br = new BufferedReader(new FileReader(sourceFile));
            String line;
            String fileToModify;
            while ((line = br.readLine()) != null) {
                fileToModify = line;
                File toRead = new File(line);
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