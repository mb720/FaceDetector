package eu.matthiasbraun;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;

public class LittleIOUtil {

	/**
	 * A regular expression matching every string.
	 */
	public static final String MATCH_ALL = ".*";
	private static final Logger LOG = LoggerFactory.getLogger(LittleIOUtil.class);

	/**
	 * Error message for dealing with IO exceptions. The braces are used by <a
	 * href="http://www.slf4j.org/">SLF4J</a> to insert the file or directory
	 * name.
	 */
	private static final String COULD_NOT_OPEN_DIR = "IOException opening directory {}";
	/**
	 * Gets all the file paths of the files inside {@code paths}.
	 * <p>
	 * If there are directories among the {@code filePaths}, collect the files inside them, recursively.
	 * <p>
	 * We won't follow symbolic links as this might lead to infinite loops.
	 * 
	 * @param paths
	 *            file paths that might contain directories
	 * @param listener
	 *            a {@link FileListener} that gets notified when we find a new file. Can be null
	 * @return
	 *         all the file paths among {@code filePaths}
	 */
	public static List<String> flatten(Collection<String> paths) {
		List<String> allFilePaths = new ArrayList<>();
		for (String filePath : paths) {
			File file = getResource(filePath, LittleIOUtil.class);
			if (file.isDirectory()) {
				// This file is a directory -> Get all files inside
				boolean recursive = true;
				Path startDir = Paths.get(file.getAbsolutePath());
				// Get all the files in this directory, recursively
				List<String> newFilePaths = listFiles(startDir, recursive, MATCH_ALL).stream().map(path -> path.toString()).collect(Collectors.toList());
				allFilePaths.addAll(newFilePaths);
			}
			else {
				// The file is not a directory
				allFilePaths.add(filePath);
			}
		}
		// Only keep the files that are not directories
		final List<String> files = allFilePaths.stream().filter(file -> !new File(file).isDirectory()).collect(Collectors.toList());
		return files;
	}

	public static List<String> flatten(String path) {
		return flatten(Arrays.asList(path));
	}

	/**
	 * @return the current directory as a {@code Path}
	 */
	public static Path getCurrDir() {
		final String currDir = StandardSystemProperty.USER_DIR.value();
		return FileSystems.getDefault().getPath(currDir, "");
	}
	/**
	 * Loads a resource and returns it as a {@code File}.
	 * <p>
	 * This will not work if the resource is inside a jar; Use {@link ClassLoader#getResourceAsStream(String)} instead.
	 *
	 * @param path
	 *            path to the resource. If the resource is in {@code src/main/resources/res.txt} the path should be {@code /res.txt}
	 * @param caller
	 *            class of the caller
	 * @return the resource as a {@code File}
	 * @see Class#getResource(String)
	 */
	public static File getResource(final String path, final Class<?> caller) {
		final URL resourceUrl = caller.getResource(path);
		File resource;
		if (resourceUrl == null) {
			LOG.warn("Did not find resource at {}", path);
			LOG.warn("Current dir: {}", getCurrDir());
			resource = new File("");
		}
		else {
			resource = new File(resourceUrl.getFile());
		}
		return resource;
	}

	public static List<Path> listFiles(final Path startDir, final boolean recursive, final String filterRegex) {
		final List<Path> paths = new ArrayList<>();
		listFiles(paths, startDir, recursive, filterRegex);

		return paths;
	}

	private static void listFiles(final List<Path> paths, final Path currDir, final boolean recursive, final String regexFilter) {
		if (currDir.toFile().isDirectory()) {
			try (DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(currDir)) {
				for (final Path path : stream) {
					final String fileName = path.toString();
					if (fileName.matches(regexFilter)) {
						paths.add(path);
					}
					/*
					 * Traversing a file system can be a lengthy task.
					 * If the executing thread is interrupted (because, for example, the user has signaled that things are taking too long),
					 * we'll not go into any further directories.
					 */
					boolean interrupted = Thread.currentThread().isInterrupted();

					/*
					 * We don't follow symbolic links as they might lead to
					 * infinite loops (e.g., when the symlink points to its
					 * parent dir)
					 */
					boolean isSymbolicLink = java.nio.file.Files.isSymbolicLink(path);
					if (recursive && !interrupted && !isSymbolicLink) {
						listFiles(paths, path, recursive, regexFilter);
					}
				}
			}
			catch (final IOException e) {
				LOG.error(COULD_NOT_OPEN_DIR, currDir, e);
			}
		}
	}
}
