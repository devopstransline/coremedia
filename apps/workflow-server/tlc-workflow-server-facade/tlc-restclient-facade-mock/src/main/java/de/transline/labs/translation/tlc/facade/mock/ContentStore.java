package de.transline.labs.translation.tlc.facade.mock;

import de.transline.labs.translation.tlc.facade.TLCFacadeIOException;
import com.google.common.io.ByteSource;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Part of mocking the content API. It will remember contents to be translated
 * until they are used within a translation submission.
 */
@DefaultAnnotation(NonNull.class)
final class ContentStore {
  private String store;

  /**
   * Adds the given content to the store.
   *
   * @param resource resource to read
   * @throws TLCFacadeIOException if the resource could not be read
   */
  void addContent(Resource resource) {
    try (InputStream is = resource.getInputStream()) {
      ByteSource source = new ByteSource() {
        @Override
        public InputStream openStream() {
          return is;
        }
      };
      synchronized (store) {
        store = source.asCharSource(StandardCharsets.UTF_8).read();
      }
    } catch (IOException e) {
      throw new TLCFacadeIOException(e, "Failed to read resource: " + resource);
    }
  }

  /**
   * Removes the content.
   *
   * @return data of the content which got removed
   */
  String removeContent() {
    synchronized (store) {
      String content = store;
      store = null;
      return content;
    }
  }

}
