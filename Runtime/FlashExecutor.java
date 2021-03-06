import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

/**
 * SER 502 Project 2 Group 31.
 * 
 * Flash Programming Language
 * 
 * Runtime Flash Executor
 * 
 */
public class FlashExecutor {

	public static void main(String args[]) {

		System.out.println("<-------------- Starting Execution -------------->\n");

		if (args.length < 1) {
			System.out.println("Arguments must contain one or more .fl.cls files");
			return;
		}

		try {
			for (String filePath : args) {
				executeFlClass(filePath);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}

		System.out.println("<-------------- End of Execution -------------->");
	}

	/**
	 * Executes the machine code depending on the the instruction pointer and environment pointer.
	 * @param filePath
	 * @throws IOException
	 */
	private static void executeFlClass(String filePath) throws IOException {

		if (!filePath.contains(".fl.cls")) {
			System.out.println("Please enter a valid intermediate file. Received " + filePath);
			return;
		}

		System.out.println("Executing file--> " + filePath.substring(filePath.lastIndexOf("\\") + 1) + "\n");

		FLConstants cmd = null;

		Stack<Object> execStack = new Stack<Object>();
		Stack<Object> callStack = new Stack<Object>();
		HashMap<String, Object> varList = new HashMap<String, Object>();
		HashMap<String, Function> functions = new HashMap<String, Function>();

		// Getting all the lines of the file as a String Array
		ArrayList<String> fileLines = new ArrayList<String>();
		getFileCount(fileLines, filePath);

		String lineScanned = "";
		Scanner scan = null;
		int skipto = 0;
		int functionRefNumber = 0;
		boolean skip = false;
		boolean skipForFunction = false;
		String functionName = "";
		ArrayList<String> params = new ArrayList<String>();

		for (int ip = 0; ip < fileLines.size(); ip++) {
			if (skip) {
				ip = skipto;
			}
			lineScanned = fileLines.get(ip);

			// Ignores line numbers
			if (lineScanned.contains(":")) {
				lineScanned = lineScanned.substring(lineScanned.indexOf(":") + 1);
			}

			if (lineScanned.contains("fun_end")) {
				skipForFunction = false;
			}
			scan = new Scanner(lineScanned);
			skip = false;
			while (!skip && !skipForFunction) {
				Integer intVal = 0;
				if (scan.hasNext()) {
					String str = scan.next();
					if (FLConstants.contains(str)) {
						cmd = FLConstants.valueOf(str.toUpperCase());
					}
					
					switch (cmd) {
					case LD_INT:
						if (scan.hasNextInt()) {
							int number = scan.nextInt();
							execStack.push(number);
						} else {
							System.out.println("Command LD_INT must be followed by an Integer");
							System.exit(1);
						}
						break;
					case LD_STR:
						String stg = "";
						while (scan.hasNext()) {
							stg = stg + " " + scan.next();
						}
						execStack.push(stg);
						break;
					case LD_BOL:
						if (scan.hasNextBoolean()) {
							boolean bool = scan.nextBoolean();
							execStack.push(bool);
						} else {
							System.out.println("Command LD_BOL must be followed by an Boolean");
							System.exit(1);
						}
						break;
					case LD_VAR:
						if (scan.hasNext()) {
							String var = scan.next() + "";
							if (varList.containsKey(var)) {
								Object val = varList.get(var);
								execStack.push(val);
							} else {
								System.out.println("Command LD_VAR must be followed by an Valid offset");
								System.out.println("Intruction Pointer at: " + ip);
								System.exit(1);
							}
						} else {
							System.out.println("Command LD_VAR must be followed by an Offset");
							System.out.println("Intruction Pointer at: " + ip);
							System.exit(1);
						}
						break;
					case STORE:
						Object value = execStack.pop();
						if (scan.hasNext()) {
							String off = scan.next();
							varList.put(off, value);
						} else {
							System.out.println("Command LD_INT must be followed by an Offset");
							System.exit(1);
						}
						break;
					case GOTO:
						if (scan.hasNext()) {
							ip = scan.nextInt() - 1;
						} else {
							System.out.println("Command GOTO must be followed by a Line Number");
							System.exit(1);
						}
						break;
					case OUT_INT:
						if (scan.hasNext()) {
							int off = scan.nextInt(); // Not Used
							System.out.println(execStack.pop() + "");
						} else {
							System.out.println("Command OUT_INT must be followed by an Offset");
							System.exit(1);
						}
						break;
					case OUT_STR:
						if (scan.hasNext()) {
							int off = scan.nextInt(); // Not Used
							String output = execStack.pop() + "";
							output = output.replace("\"", "");
							System.out.println(output.trim());
						} else {
							System.out.println("Command OUT_STR must be followed by an Offset");
							System.exit(1);
						}
						break;
					case OUT_BOL:
						if (scan.hasNext()) {
							int off = scan.nextInt(); // Not Used
							System.out.println(execStack.pop());
						} else {
							System.out.println("Command OUT_BOL must be followed by an Offset");
							System.exit(1);
						}
						break;
					case ADD:
						String n1 = execStack.pop().toString();
						String n2 = execStack.pop().toString();
						String o1 = n1;
						String o2 = n2;
						if (!o1.contains("\"") && !(o2.contains("\"")) && !o1.matches("\\D+") && !o2.matches("\\D+")) {
							int d1 = Integer.valueOf(o1);
							int d2 = Integer.valueOf(o2);
							execStack.push(d2 + d1);
						} else {
							execStack.push("\"" + o2.replaceAll("\"", "").concat(o1.replaceAll("\"", "")) + "\"");
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case CALL:
						functionName = scan.next();
						HashMap<String, Object> tmpList = new HashMap<String, Object>();
						params = new ArrayList<String>();
						while (true) {
							lineScanned = fileLines.get(++ip);

							// Ignores line number
							if (lineScanned.contains(":")) {
								lineScanned = lineScanned.substring(lineScanned.indexOf(":") + 1);
							}

							scan = new Scanner(lineScanned);
							String command = scan.next();
							if ("arg".equals(command)) {
								params.add(scan.next());
							} else {
								while (scan.hasNext()) {
									scan.next();
								}
								break;
							}
						}
						ip--;
						for (String s : params) {
							tmpList.put(s, varList.get(s));
						}
						callStack.push(varList.clone());
						callStack.push(ip);
						ip = functions.get(functionName).lineNum;
						assignParamValues(functions.get(functionName).params, params, varList, tmpList);
						break;
					case JMP_FALSE:
						skipto = scan.nextInt();
						if (!(Boolean) execStack.pop()) {
							skip = true;
						} else {
							skipto = ip;
						}
						break;
					case DIVIDE:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						if (!o1.contains("\"") || !(o2.contains("\""))) {
							int d1 = Integer.valueOf(o1);
							int d2 = Integer.valueOf(o2);
							execStack.push(d2 / d1);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case HALT:
						System.out.println("\nEnd of program\n");
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case FUN_END:

						if (!callStack.isEmpty()) {
							ip = (Integer) callStack.pop();
							varList = (HashMap<String, Object>) callStack.pop();
							// num = returnLineNumber;
						} else {
							Function fun = new Function(functionName, functionRefNumber, params);
							functions.put(functionName, fun);
						}
						skipForFunction = false;
						if (scan.hasNext()) {
							scan.next(); // Skips
						}
						break;
					case FUN_INIT:
						functionName = scan.next();
						params = new ArrayList<String>();

						while (true) {
							lineScanned = fileLines.get(++ip);

							// Ignores line number
							if (lineScanned.contains(":")) {
								lineScanned = lineScanned.substring(lineScanned.indexOf(":") + 1);
							}

							scan = new Scanner(lineScanned);
							String command = scan.next();
							if ("def".equals(command)) {
								continue;
							}
							if ("para_int".equals(command)) {
								params.add(scan.next());
							} else {
								break;
							}
						}
						ip--;
						skipForFunction = true;
						functionRefNumber = ip;
						break;
					case GT:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						int d1 = Integer.valueOf(o1);
						int d2 = Integer.valueOf(o2);
						execStack.push(d2 > d1);
						String tmp = scan.next(); // Not used
						break;
					case EQ:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						d1 = Integer.valueOf(o1);
						d2 = Integer.valueOf(o2);
						execStack.push(d2 == d1);
						tmp = scan.next(); // Not used
						break;
					case GTEQ:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						d1 = Integer.valueOf(o1);
						d2 = Integer.valueOf(o2);
						execStack.push(d2 >= d1);
						tmp = scan.next(); // Not used
						break;
					case LT:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						d1 = Integer.valueOf(o1);
						d2 = Integer.valueOf(o2);
						execStack.push(d2 < d1);
						tmp = scan.next(); // Not used
						break;
					case LTEQ:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						d1 = Integer.valueOf(o1);
						d2 = Integer.valueOf(o2);
						execStack.push(d2 <= d1);
						tmp = scan.next(); // Not used
						break;
					case MULT:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						if (!o1.contains("\"") || !(o2.contains("\""))) {
							d1 = Integer.valueOf(o1);
							d2 = Integer.valueOf(o2);
							execStack.push(d2 * d1);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case POP:
						ip = (Integer) callStack.pop();
						varList = (HashMap<String, Object>) callStack.pop();
						if (scan.hasNext()) {
							scan.next();
						} 
						break;
					case SUB:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						if (!o1.contains("\"") || !(o2.contains("\""))) {
							d1 = Integer.valueOf(o1);
							d2 = Integer.valueOf(o2);
							execStack.push(d2 - d1);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case PWR:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						o1 = n1;
						o2 = n2;
						if (!o1.contains("\"") || !(o2.contains("\""))) {
							d1 = Integer.valueOf(o1);
							d2 = Integer.valueOf(o2);
							int ans = (int) Math.pow(d2, d1);
							execStack.push(ans);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case DATA:
						break;
					case IN_INT:
						System.out.println("Enter a number");
						Scanner sc = new Scanner(System.in);
						try {
							int inp = sc.nextInt();

							String var = scan.next() + "";
							if (!"".equals(var)) {
								varList.put(var, inp);
							}
						} catch (InputMismatchException e) {
							System.out.println("Input is not a valid integer. Exiting ....");
							System.exit(1);
						}
						break;
					case IN_STR:
						System.out.println("Enter a string");
						sc = new Scanner(System.in);
						try {
							String inp = sc.next();

							String var = scan.next() + "";
							if (!"".equals(var)) {
								varList.put(var, inp);
							}
						} catch (InputMismatchException e) {
							System.out.println("Input is not a valid string. Exiting ....");
							System.exit(1);
						}
						break;
					case IN_BOL:
						System.out.println("Enter a boolean");
						sc = new Scanner(System.in);
						try {
							boolean inpB = sc.nextBoolean();
							String var = scan.next() + "";
							if (!"".equals(var)) {
								varList.put(var, inpB);
							}
						} catch (InputMismatchException e) {
							System.out.println("Input is not a valid boolean. Exiting ....");
							System.exit(1);
						}
						break;
					case DEF:
						if (scan.hasNext()) {
							String ob = scan.next();
							if (ob.contains("str")) {
								String v[] = ob.split("%");
								varList.put(v[0], "");
								break;
							} else if (ob.contains("stk")) {
								Stack<Integer> st = new Stack<Integer>();
								String v[] = ob.split("%");
								varList.put(v[0], st);
								break;
							} else {
								String v[] = ob.split("%");
								varList.put(v[0], v[1]);
							}
						} else {
							System.out.println("DEF must be followed by default value");
							System.exit(1);
						}
						break;
					case ADDSTK:
						if (scan.hasNext()) {
							String offset = scan.next();
							Stack<Integer> st = (Stack<Integer>) varList.get(offset);
							st.push((int) execStack.pop());
						} else {
							System.out.println("ADDSTK must be followed by an offset");
							System.exit(1);
						}
						break;
					case REMSTK:
						if (scan.hasNext()) {
							String offset = scan.next();
							Stack<Integer> st = (Stack<Integer>) varList.get(offset);
							execStack.push(st.pop());
						} else {
							System.out.println("REMSTK must be followed by an offset");
							System.exit(1);
						}
						break;
					case AND:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						boolean b1;
						boolean b2;
						if (!n1.contains("\"") || !(n2.contains("\""))) {
							b1 = Boolean.valueOf(n1);
							b2 = Boolean.valueOf(n2);
							execStack.push(b2 && b1);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					case OR:
						n1 = execStack.pop().toString();
						n2 = execStack.pop().toString();
						if (!n1.contains("\"") || !(n2.contains("\""))) {
							b1 = Boolean.valueOf(n1);
							b2 = Boolean.valueOf(n2);
							execStack.push(b2 || b1);
						}
						if (scan.hasNext()) {
							int tmp1 = scan.nextInt(); // Not Used
						}
						break;
					default:
						System.out.println("Command not recognized: " + cmd);
						System.exit(1);
						break;
					}
				} else {
					break;
				}
			}
			scan.close();
		}
	}

	/**
	 * Reads the file and returns the a string array of lines of code.
	 * 
	 * @param lines
	 * @param filePath
	 * @return ArrayList<String>
	 * @throws IOException
	 */
	private static int getFileCount(ArrayList<String> lines, String filePath) throws IOException {
		InputStream is = new FileInputStream(filePath);
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		String line = "";
		int count = 0;
		while ((line = br.readLine()) != null) {
			count++;
			lines.add(line);
		}
		br.close();
		is.close();
		return count;
	}

	private static void assignParamValues(List<String> funParams, List<String> params, HashMap<String, Object> vList,
			HashMap<String, Object> tmpList) {
		for (int i = 0; i < funParams.size(); i++) {
			vList.put(funParams.get(i), tmpList.get(params.get(i)));
		}
	}

}
