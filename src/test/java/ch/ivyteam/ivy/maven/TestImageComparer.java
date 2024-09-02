package ch.ivyteam.ivy.maven;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestImageComparer {

  private LogCollector log;

  private File refConditionTab;
  private File newConditionTab;

  @TempDir
  private Path refRoot;

  @TempDir
  private Path newRoot;

  @BeforeEach
  void setUp() throws IOException {
    log = new LogCollector();
    refRoot = Files.createTempDirectory("refRoot");
    newRoot = Files.createTempDirectory("newRoot");
    refConditionTab = load(refRoot, "refImg/1-Condition Tab.png");
    newConditionTab = load(newRoot, "newImg/1-Condition Tab.png");
  }

  @Test
  void compareIdenticalImages() {
    new ImageComparer(refRoot.toFile(), refRoot.toFile(), asList(refConditionTab), 99.99f, log).compare();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getDebug().iterator().next().toString()).startsWith("comparing")
            .contains("1-Condition Tab.png");
  }

  @Test
  void compareDifferentImages() {
    compare(newConditionTab);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Image only has similarity of");
  }

  @Test
  void compareDifferentSizedImages() throws IOException {
    load(refRoot, "refImg/0-Name Tab.png");
    File newNameTab = load(newRoot, "newImg/0-Name Tab.png");
    compare(newNameTab);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Different sized image:");
  }

  @Test
  void compareNonExistentImages() {
    File nonExistentImage = new File(newConditionTab.getParent(), "NonExistentImage.png");
    compare(nonExistentImage);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Could not read image:");
  }

  private void compare(File nonExistentImage) {
    new ImageComparer(refRoot.toFile(), newRoot.toFile(), asList(nonExistentImage), 99.99f, log).compare();
  }

  private File load(Path rootDir, String testRes) throws IOException {
    try (var in = TestImageComparer.class.getResourceAsStream("../../../../" + testRes)) {
      File elementDir = new File(rootDir.toFile(), "MyElement");
      elementDir.mkdir();
      File img = new File(elementDir, new File(testRes).getName());
      Files.copy(in, img.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return img;
    }
  }
}
