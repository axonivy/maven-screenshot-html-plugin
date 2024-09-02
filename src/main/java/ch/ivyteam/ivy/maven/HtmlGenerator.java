package ch.ivyteam.ivy.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.maven.plugin.logging.Log;

public class HtmlGenerator {

  private final String templateHtml;
  private final String artifactTargetPath;
  private final List<File> imageFiles;
  private final Path rootDir;
  private final Log log;
  private StringBuilder imgTagBuilder;
  private String lastParent;
  private String rootRelativePath;
  private String referenceRelativePath;

  public HtmlGenerator(String template, String artifactTargetPath, List<File> imageFiles, Path rootDir,
          Log log, String rootRelativePath, String referenceRelativePath) {
    this.templateHtml = template;
    this.artifactTargetPath = artifactTargetPath;
    this.imageFiles = imageFiles;
    this.rootDir = rootDir;
    this.rootRelativePath = rootRelativePath == null ? "" : rootRelativePath;
    this.referenceRelativePath = referenceRelativePath == null ? "" : referenceRelativePath;
    this.log = log;
  }

  public String generate() {
    imgTagBuilder = new StringBuilder();
    imageFiles.stream().forEach(this::appendImage);
    String filledTemplate = templateHtml.replace(GenerateImageHtmlMojo.REPLACE_TAG_IMG, imgTagBuilder.toString());
    return filledTemplate.replace(GenerateImageHtmlMojo.REPLACE_TAG_TARGET_PATH, artifactTargetPath + referenceRelativePath);
  }

  private void appendImage(File image) {
    Path relativeImagePath = rootDir.relativize(image.toPath());
    appendTitle(relativeImagePath);
    log.debug("Adding: " + relativeImagePath);
    imgTagBuilder.append("<img src=\"" + rootRelativePath + rootDir.getFileName() + "/" + relativeImagePath
            + "\" title=\"" + image.getName() + "\">\n");
  }

  private void appendTitle(Path relativeImagePath) {
    String imgParent = relativeImagePath.getParent().toString();
    if (!Objects.equals(imgParent, lastParent)) {
      lastParent = imgParent;
      String title = relativeImagePath.getParent().toString();
      String id = title.replace(" ", "-");
      imgTagBuilder.append("<h3 id='" + id + "'>" + title + "</h3><a href='#" + id + "'>link</a></br>\n");
    }
  }
}
