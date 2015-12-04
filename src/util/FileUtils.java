/**
 * 
 */
package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import constants.ConfigConstant;

/**
 * @author sumit
 *
 */
public class FileUtils {

	public static String[] getAllFiles(String pDirectoryPath) {
		return new File(pDirectoryPath).list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isFile();
			}
		});
	}

	private static String getText(String filePath, Charset chset)
			throws IOException {
		chset = getCharSet(chset);

		/*
		 * StringBuilder build = new StringBuilder(); String newLine =
		 * System.getProperty("line.seperator"); if(chset==null) chset =
		 * StandardCharsets.UTF_8;
		 * 
		 * try (BufferedReader bfrdRead = Files.newBufferedReader(new File(
		 * filePath).toPath(), chset)) { String text = null; while ((text =
		 * bfrdRead.readLine()) != null) { build.append(text);
		 * build.append(newLine); } } return build.toString();
		 */
		return String.join(ConfigConstant.NEW_LINE_CHAR, Files.readAllLines(
				Paths.get(new File(filePath).toURI()), chset));
	}

	private static Charset getCharSet(Charset chset) {
		if (chset == null)
			chset = ConfigConstant.DEFAULT_CHAR_SET;
		return chset;
	}

	public static void flushToFile(FileWriter fileWriter, String text)
			throws IOException {
		fileWriter.write(text);
		fileWriter.flush();
	}

	public static void writeToFile(String text, String fileName)
			throws IOException {
		try (BufferedWriter bfrdWriter = Files.newBufferedWriter(Paths
				.get(fileName))) {
			bfrdWriter.write(text);
		}
	}

	public static boolean exists(String locn) {
		return new File(locn).exists();
	}

	public static boolean makeDirectory(String location) {
		return new File(location).mkdirs();
	}

	public static FileWriter[] getPoolOfFileWriter(int fileResoursePoolSize,
			String fileLocationPrefix) {
		List<FileWriter> fileWriterPool = new ArrayList<FileWriter>();

		for (int i = 0; i < fileResoursePoolSize; i++) {
			try {
				fileWriterPool.add(new FileWriter(new File(fileLocationPrefix
						+ ConfigConstant.UNDERSCORE + i)));
			} catch (IOException ex) {
				cleanUpFileWriterPool(fileWriterPool.toArray(new FileWriter[0]));
			}
		}
		return fileWriterPool.toArray(new FileWriter[0]);
	}

	public static void cleanUpFileWriterPool(FileWriter[] pool) {
		for (FileWriter f : pool) {
			try {
				f.close();
			} catch (IOException ex) {
				// ex.printStackTrace();
			}
		}
	}

	public static String readAllLines(String filePath, Charset chset)
			throws IOException {
		return getText(filePath, chset);
	}

	public static Stream<String> readAllLinesOptimized(String filePath,
			Charset chset) throws IOException {
		return Files.readAllLines(new File(filePath).toPath(),
				getCharSet(chset)).stream();

	}

	public static void writeLinesToFile(Collection<?> texts, String filePath)
			throws IOException {
		try (BufferedWriter wrtr = Files.newBufferedWriter(new File(filePath)
				.toPath())) {

			for (Object t : texts) {
				wrtr.write(t.toString());
				wrtr.newLine();
				wrtr.flush();
			}
		}
	}

	public static void writeToFileWithNewLine(BufferedWriter bfrdWriter,
			String text) throws IOException {
		bfrdWriter.write(text);
		bfrdWriter.newLine();
		bfrdWriter.flush();

	}

	/**
	 * 
	 * @param sourceDir
	 *            - directory location ended with / (file path seperator)
	 * @param fileName
	 *            - child file name or directory name.
	 * @return
	 */
	public static String getFullPath(String sourceDir, String fileName) {
		return sourceDir + fileName;
	}

	public static void createDirectory(String... dirs) {
		for (String dir : dirs) {
			makeDirectory(dir);
		}

	}
}
