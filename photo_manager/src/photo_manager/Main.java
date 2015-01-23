package photo_manager;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class Main {
	private static Set<String> extensions = new HashSet<String>();
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost/photo_collection";
	private static final String USER = "root";
	private static final String PASS = "a5h2o1";
	private static Connection connection;
	private static MessageDigest messageDigest;
	private static long count = 0;
	private static Map<String, String> fileHashes = new HashMap<String, String>();

	private static FileFilter dirFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory() && !pathname.getName().startsWith(".");
		}
	};
	private static FileFilter jpegFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.toString().endsWith("jpg");
		}
	};

	static {
		try {
			Class.forName(JDBC_DRIVER);
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			messageDigest = MessageDigest.getInstance("MD5");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select path, id from photo");
			while (resultSet.next()) {
				String path = resultSet.getString(1);
				String id = resultSet.getString(2);
				fileHashes.put(id, path);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		processExif();
	}

	public static void calcPhotos(File rootDir) {
		int count = 0;
		File[] photoDirs = rootDir.listFiles(dirFilter);
		for (int i = 0; i < photoDirs.length; i++) {
			System.out.println(photoDirs[i]);
			File[] photos = photoDirs[i].listFiles(jpegFilter);
			count += photos.length;
		}
		System.out.println(count);
	}
	
	public static void readExif() {
		try {
			Statement statement = connection.createStatement();
			PreparedStatement preparedStatement = connection.prepareStatement("update photo set metadata = ? where id = ?");
			ResultSet resultSet = statement.executeQuery("select id, path from photo");
			while (resultSet.next()) {
				String path = resultSet.getString(2);
				try {
					Metadata imageMetadata = ImageMetadataReader.readMetadata(new File(path));
					JSONObject jsonMetadata = new JSONObject();
					for (Directory directory : imageMetadata.getDirectories()) {
						JSONObject jsonTags = new JSONObject();
						for(Tag tag : directory.getTags()) {
							jsonTags.put(tag.getTagName(), tag.getDescription());
						}
						jsonMetadata.put(directory.getName(), jsonTags);
					}
					preparedStatement.setString(1, jsonMetadata.toString());
					int id = resultSet.getInt(1);
					preparedStatement.setInt(2, id);
					preparedStatement.addBatch();
					System.out.println(id);
				} catch (ImageProcessingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			preparedStatement.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void processExif() {
		try {
			FileInputStream tagNamesFile = new FileInputStream(new File("metadata" + File.separator + "all_tag_names"));
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(tagNamesFile));
			List<String> tagNames = new ArrayList<String>();
			String line = bufferedReader.readLine();
			while (line != null) {
				tagNames.add(line);
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
			tagNamesFile.close();
			System.exit(0);
			SevenZFile allPhotosFile = new SevenZFile(new File("metadata" + File.separator + "all_photos_metadata.7z"));
			SevenZArchiveEntry entry = allPhotosFile.getNextEntry();
			byte[] b = new byte[(int) entry.getSize()];
			allPhotosFile.read(b, 0, b.length);
			JSONObject allPhotos = new JSONObject(new String(b));
			for (String id : allPhotos.keySet()) {
				JSONObject jsonMetadata = allPhotos.getJSONObject(id);
				for (String directory : jsonMetadata.keySet()) {
					JSONObject jsonTags = jsonMetadata.getJSONObject(directory);
					System.out.println(jsonTags);
				}
			}
			allPhotosFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void addPairs(File rootDir) {
		try {
			Statement statement = connection.createStatement();
			PreparedStatement updateStatement = connection.prepareStatement("insert into pair (first, second) values (?, ?)");
			ResultSet resultSet = statement.executeQuery("select count(*) as pairs, camcorder_id from photo group by camcorder_id having camcorder_id order by pairs");
			double total_speed = 0;
			int counter = 0;
			count = 0;
			int totalPairs = 15713532;
			String notIn = " (";
			while (resultSet.next()) {
				int files = resultSet.getInt(1);
				if (files >= 2) {
					int camcorder = resultSet.getInt(2);
					long startTime = System.nanoTime();
					long pairs = CombinatoricsUtils.binomialCoefficient(files, 2);
					count += pairs;
					double avg_speed = total_speed / counter;
					int estimateTime = (int) (pairs / avg_speed);
					Date estDate = new Date();
					estDate.setTime(estDate.getTime() + (long) ((pairs / avg_speed) * 1000));
					Date finishDate = new Date();
					finishDate.setTime(finishDate.getTime() + (long) ((totalPairs / avg_speed) * 1000));
					System.out.println(String.format("Estimated time for %d pairs:\t\t%d\tseconds\t\tTill: %s\tFinish: %s", pairs, estimateTime, estDate.toString(), finishDate.toString()));
					statement = connection.createStatement();
					ResultSet filesResultSet = statement.executeQuery("select id from photo where camcorder_id = " + camcorder);
					List<Integer> ids = new ArrayList<Integer>();
					while (filesResultSet.next()) {
						ids.add(filesResultSet.getInt(1));
					}
					int batch_counter = 0;
					final int batch_limit = 1000000;
					for (int i = 0; i < ids.size(); i++) {
						for (int j = i + 1; j < ids.size(); j++) {
							updateStatement.setInt(1, ids.get(i));
							updateStatement.setInt(2, ids.get(j));
							updateStatement.addBatch();
							batch_counter++;
							if (batch_counter > batch_limit) {
								updateStatement.executeBatch();
								batch_counter = 0;
							}
						}
					}
					updateStatement.executeBatch();
					totalPairs -= pairs;
					double duration = (System.nanoTime() - startTime) / 1000000000.0;
					total_speed += (pairs / duration);
					counter++;
					notIn += camcorder + ", ";
					System.out.println(String.format("%d\t\t%d\t\t\t%.2f\tseconds\t\t%.2f pairs per second\ttotal pairs: %d\tinserted camcorders:%s", pairs, camcorder, duration, (pairs / duration), count, notIn));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void databaseModification() {
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select id, md5 from hash");
			Map<String, Integer> hashes = new HashMap<String, Integer>();
			while (resultSet.next()) {
				hashes.put(resultSet.getString(2), resultSet.getInt(1));
			}
			int rowCount = 15713532;
			int limit = 1;
			int offset = 0;
			PreparedStatement updateStatement = connection.prepareStatement("update pair set first = ?, second = ? where first = ? and second = ?");
			while (offset < rowCount) {
				long startTime = System.nanoTime();

				resultSet = statement.executeQuery("select first, second from pair limit " + offset + ", " + limit);
				while (resultSet.next()) {
					String first = resultSet.getString(1);
					String second = resultSet.getString(2);
					updateStatement.setInt(1, hashes.get(first));
					updateStatement.setInt(2, hashes.get(second));
					updateStatement.setString(3, first);
					updateStatement.setString(4, second);
					updateStatement.addBatch();
				}
				updateStatement.executeBatch();
				offset += limit;
				double duration = (System.nanoTime() - startTime) / 1000000000.0;
				System.out.println(offset + ".\t" + duration + " seconds");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void compareAllPhotos(File rootDir) {
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = null;
			PreparedStatement updateStatement = connection.prepareStatement("update pair set matches = ? where first = ? and second = ?");
			for (int i = 0; i < 1000000; i++) {
				long startTime = System.nanoTime();
				resultSet = statement.executeQuery("select first, second from pair where matches is null limit 0, 10");
				// int counter = 0;
				while (resultSet.next()) {
					// counter++;
					String first = resultSet.getString(1);
					String second = resultSet.getString(2);
					double d = comparePhotos(fileHashes.get(first), fileHashes.get(second));
					updateStatement.setDouble(1, d);
					updateStatement.setString(2, first);
					updateStatement.setString(3, second);
					updateStatement.addBatch();
					// if (d > 0.5) {
					// System.out.println(String.format("%d\t%.12f\t%s %s", counter, d, fileHashes.get(first), fileHashes.get(second)));
					// } else {
					// System.out.println(counter + "\t" + d);
					// }
				}
				updateStatement.executeBatch();
				double duration = (System.nanoTime() - startTime) / 1000000000.0;
				System.out.println(i + ".\t" + duration + " seconds");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static double comparePhotos(String photo1, String photo2) {
		BufferedImage img1 = null;
		BufferedImage img2 = null;
		try {
			img1 = ImageIO.read(new File(photo1));
			img2 = ImageIO.read(new File(photo2));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int width1 = img1.getWidth(null);
		int width2 = img2.getWidth(null);
		int height1 = img1.getHeight(null);
		int height2 = img2.getHeight(null);
		if ((width1 != width2) || (height1 != height2)) {
			return 0;
		}
		long diff = 0;
		for (int y = 0; y < height1; y++) {
			for (int x = 0; x < width1; x++) {
				int rgb1 = img1.getRGB(x, y);
				int rgb2 = img2.getRGB(x, y);
				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >> 8) & 0xff;
				int b1 = (rgb1) & 0xff;
				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >> 8) & 0xff;
				int b2 = (rgb2) & 0xff;
				diff += Math.abs(r1 - r2);
				diff += Math.abs(g1 - g2);
				diff += Math.abs(b1 - b2);
			}
		}
		double n = width1 * height1 * 3;
		double p = diff / n / 255.0;
		return p;
	}

	public static void addPhotos(File rootDir) {
		try {
			File[] photoDirs = rootDir.listFiles(dirFilter);
			PreparedStatement statement = connection.prepareStatement("insert into photo (id, path) values (?, ?)");
			Statement select = connection.createStatement();
			for (int i = 0; i < photoDirs.length; i++) {
				System.out.println(photoDirs[i]);
				File[] photos = photoDirs[i].listFiles(jpegFilter);
				for (int j = 0; j < photos.length; j++) {
					String md5 = md5(photos[j]);
					String photoPath = photos[j].getAbsolutePath();
					statement.setString(1, md5);
					statement.setString(2, photoPath);
					try {
						statement.executeUpdate();
					} catch (MySQLIntegrityConstraintViolationException e) {
						ResultSet resultSet = select.executeQuery("select path from photo where id = '" + md5 + "'");
						resultSet.next();
						System.out.println("   ---   " + resultSet.getString(1));
						System.out.println("   ---   " + photoPath);
						System.out.println("   ---   " + (photos[j].delete() ? "deleted" : "not deleted"));
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String md5(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] dataBytes = new byte[1024];
		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			messageDigest.update(dataBytes, 0, nread);
		}
		fis.close();
		byte[] mdbytes = messageDigest.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	public static void addCamcorders(File rootDir) {
		try {
			File[] photoDirs = rootDir.listFiles(dirFilter);
			PreparedStatement statement = connection.
					prepareStatement("insert into camcorder (name) values (?)");
			for (int i = 0; i < photoDirs.length; i++) {
				String camcorder = photoDirs[i].getName();
				statement.setString(1, camcorder);
				try {
					statement.executeUpdate();
				} catch (MySQLIntegrityConstraintViolationException e) {
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	public static void recursiveRun(File path) {
		if (path.isDirectory()) {
			File[] subFiles = path.listFiles();
			for (int i = 0; i < subFiles.length; i++) {
				recursiveRun(subFiles[i]);
			}
		} else {
			String name = path.getName();
			if (name.indexOf('.') != -1) {
				extensions.add(name.split("\\.")[1]);
				if ("jpg".equals(name.split("\\.")[1])) {
					count++;
				}
			}
		}
	}

	public static void setCamcorderIds() {
		try {
			Statement statement = connection.createStatement();
			PreparedStatement preparedStatement = connection.prepareStatement("update photo set camcorder_id = ? where id = ?");
			Map<String, Integer> camcorders = new HashMap<String, Integer>();
			ResultSet resultSet = statement.executeQuery("select id, name from camcorder");
			while (resultSet.next()) {
				camcorders.put(resultSet.getString(2), resultSet.getInt(1));
			}
			resultSet = statement.executeQuery("select path, id from photo");
			while (resultSet.next()) {
				String path = resultSet.getString(1);
				String id = resultSet.getString(2);
				fileHashes.put(path, id);
				String[] tokens = path.split(File.separator);
				preparedStatement.setInt(1, camcorders.get(tokens[tokens.length - 2]));
				preparedStatement.setString(2, id);
				preparedStatement.executeUpdate();
			}
			System.out.println("complete");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
