package master.distribution;

import master.bucket.BucketInfo;
import master.bucket.BucketizationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GeneratedBucketLocator {
    public List<Path> locate(BucketizationResult lastResult) throws IOException {
        if (lastResult != null && lastResult.isSuccess() && !lastResult.getBuckets().isEmpty()) {
            List<Path> paths = new ArrayList<Path>();
            for (BucketInfo bucket : lastResult.getBuckets()) {
                paths.add(bucket.getPath());
            }
            Collections.sort(paths);
            return paths;
        }
        Path latest = latestGeneratedBucketDirectory();
        if (latest == null) {
            return Collections.emptyList();
        }
        return csvFiles(latest);
    }

    private Path latestGeneratedBucketDirectory() throws IOException {
        Path root = resolveBuildDirectory().resolve("tmp").resolve("generated-buckets");
        if (!Files.exists(root)) {
            return null;
        }
        List<Path> directories = new ArrayList<Path>();
        java.util.stream.Stream<Path> stream = Files.list(root);
        try {
            stream.filter(Files::isDirectory).forEach(directories::add);
        } finally {
            stream.close();
        }
        if (directories.isEmpty()) {
            return null;
        }
        Collections.sort(directories, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                try {
                    return Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left));
                } catch (IOException exception) {
                    return right.toString().compareTo(left.toString());
                }
            }
        });
        return directories.get(0);
    }

    private List<Path> csvFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<Path>();
        java.util.stream.Stream<Path> stream = Files.list(directory);
        try {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .forEach(files::add);
        } finally {
            stream.close();
        }
        Collections.sort(files);
        return files;
    }

    private Path resolveBuildDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("MasterNode".equalsIgnoreCase(current.getFileName().toString())) {
            return current.resolve("build");
        }
        return current.resolve("MasterNode").resolve("build");
    }
}
