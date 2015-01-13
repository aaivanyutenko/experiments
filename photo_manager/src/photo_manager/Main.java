package photo_manager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class Main {
	private static Set<String> extensions = new HashSet<String>();
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost/photo_collection";
	private static final String USER = "root";
	private static final String PASS = "a5h2o1";
	private static Connection connection;
	private static MessageDigest messageDigest;

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
		addPhotos(rootDir);
	}

	public static void addPhotos(File rootDir) {
		try {
			File[] photoDirs = rootDir.listFiles(dirFilter);
			PreparedStatement statement = connection.prepareStatement("insert into photo (md5id, path) values (?, ?)");
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
						ResultSet resultSet = select.executeQuery("select path from photo where md5id = '" + md5 + "'");
						resultSet.next();
						System.out.println(resultSet.getString(1));
						System.out.println(photoPath);
						System.out.println();
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
			} else {
				System.out.println(name);
			}
		}
	}

}
