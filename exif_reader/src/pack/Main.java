package pack;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class Main {

	public static void main(String[] args) {
		try {
			List<String> tags = Arrays.asList("Make", "Model", "Date/Time Original", "Date/Time Digitized");
			String[] files = new File(".").list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					name = name.toLowerCase();
					if (name.endsWith("jpg") || name.endsWith("jpeg")) {
						return true;
					}
					return false;
				}
			});
			for (int i = 0; i < files.length; i++) {
				Metadata metadata = JpegMetadataReader.readMetadata(new File(files[i]));
//				for (Directory directory : metadata.getDirectories()) {
//					for (Tag tag : directory.getTags()) {
//						System.out.println(tag);
//						System.out.println(tag.getTagName());
//					}
//				}
				System.out.println("----------------" + files[i]);
				for (Directory directory : metadata.getDirectories()) {
					for (Tag tag : directory.getTags()) {
						if (tags.contains(tag.getTagName())) {
							System.out.println(tag);
						}
					}
				}
			}
		} catch (ImageProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
