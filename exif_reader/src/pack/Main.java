package pack;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class Main {
	private static List<String> tags = Arrays.asList("Make", "Model", "Date/Time Original", "Date/Time Digitized");
	private static Set<String> models = new HashSet<>();

	public static void main(String[] args) {
		try {
			runAllFiles(new File("/data/images"));
			System.out.println(models);
		} catch (ImageProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runAllFiles(File dir) throws JpegProcessingException, IOException {
		System.out.println("Processing dir: " + dir.getAbsolutePath() + "...");
		File[] subDirs = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				return false;
			}
		});
		for (int i = 0; i < subDirs.length; i++) {
			runAllFiles(subDirs[i]);
		}
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String name = pathname.getName().toLowerCase();
				if (name.endsWith("jpg") || name.endsWith("jpeg")) {
					return true;
				}
				return false;
			}
		});
		for (int i = 0; i < files.length; i++) {
			Metadata metadata = JpegMetadataReader.readMetadata(files[i]);
			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {
					if (tags.contains(tag.getTagName())) {
						if ("Make".equals(tag.getTagName())) {
							models.add(tag.getDescription());
						}
					}
				}
			}
		}
	}

}
