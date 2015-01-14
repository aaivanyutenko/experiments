package photo_manager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.math3.util.CombinatoricsUtils;

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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		File rootDir = new File("/home/anton/Pictures/Photo_collection");
		compareAllPhotos(rootDir);
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

	public static void compareAllPhotos(File rootDir) {
		Set<Double> diffs = new HashSet<Double>();
		File[] photoDirs = rootDir.listFiles(dirFilter);
		for (int i = 0; i < photoDirs.length; i++) {
			File[] photos = photoDirs[i].listFiles(jpegFilter);
			count = 0;
			double pairsCount = CombinatoricsUtils.binomialCoefficient(photos.length, 2);
			System.out.println(photoDirs[i].getName() + " --- " + pairsCount);
			double time = 0;
			for (int j = 0; j < photos.length; j++) {
				for (int k = j + 1; k < photos.length; k++) {
					long startTime = System.nanoTime();
					double d = comparePhotos(photos[j], photos[k]);
					long endTime = System.nanoTime();
					double duration = (endTime - startTime);
					duration /= 1000000000;
					count++;
					time += duration;
					double est = (pairsCount / count - 1) * time / 3600.0;
					System.out.println(est);
					diffs.add(d);
				}
			}
			System.out.println(diffs);
		}
	}

	public static double comparePhotos(File photo1, File photo2) {
		BufferedImage img1 = null;
		BufferedImage img2 = null;
		try {
			img1 = ImageIO.read(photo1);
			img2 = ImageIO.read(photo2);
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
		return p * 100.0;
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
			PreparedStatement statement = connection.prepareStatement("insert into camcorder (name) values (?)");
			for (int i = 0; i < photoDirs.length; i++) {
				String camcorder = photoDirs[i].getName();
				File[] photos = photoDirs[i].listFiles(jpegFilter);
				for (int j = 0; j < photos.length; j++) {
				}
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

}
