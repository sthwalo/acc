# PDFBox Font and Container Guidance

## PDFBox Font Requirements for FIN Financial Management System

Apache PDFBox requires access to system fonts for text extraction and PDF generation. In containerized or minimal environments, missing fonts or misconfigured font cache can cause failures (NoClassDefFoundError, ExceptionInInitializerError).

### Container Setup Checklist

- **Install basic fonts:**
  - For Debian/Ubuntu-based images:
    ```sh
    apt-get update && apt-get install -y fontconfig fonts-dejavu-core
    ```
  - For Alpine-based images:
    ```sh
    apk add --no-cache fontconfig ttf-dejavu
    ```
- **Set JVM to headless mode:**
  - Already handled by the application, but can be set explicitly:
    ```sh
    export JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
    ```
- **Font cache directory:**
  - The application sets `pdfbox.fontcache` to `/tmp/pdfbox-fontcache` for safe, writable cache.
- **Verify font presence:**
  - Check `/usr/share/fonts` or `/usr/local/share/fonts` in your container.

### Troubleshooting

- If you see errors like `NoClassDefFoundError: Could not initialize class org.apache.pdfbox.pdmodel.font.FontMapperImpl$DefaultFontProvider`, it means PDFBox cannot find or initialize fonts.
- The application will automatically fall back to OCR extraction if PDFBox is unavailable, but for best results, ensure fonts are installed.

### Health Check

- The `/actuator/health` endpoint (if enabled) or logs will show PDFBox health status as detected by `PdfBoxConfigurator`.

---

For more details, see the [PDFBox documentation](https://pdfbox.apache.org/2.0/commandline.html#fonts) and your container base image documentation.
