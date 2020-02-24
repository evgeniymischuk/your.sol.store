package controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class CloudController {

    @RequestMapping("/cloud")
    public String cloud(Model model) {
        final Map<String, String> albums = new LinkedHashMap<>();
        model.addAttribute("albumsMap", albums);
        File directory = new File("cloud/");
        if (directory.exists()) {
            final File[] files = directory.listFiles();
            if (files == null) {
                return "cloudMain";
            }
            for (File file : files) {
                final String fileName = file.getName();
                if (!(fileName.contains(".") || fileName.contains("-web"))) {
                    try {
                        albums.put("http://yoursol.store/cloud/" + fileName, new String(Base64.getDecoder().decode(fileName.getBytes()), StandardCharsets.UTF_8));
                    } catch (Exception ignored) {
                        albums.put("http://yoursol.store/cloud/" + fileName, fileName);
                    }
                }
            }
        }
        return "cloudMain";
    }

    @RequestMapping("/cloud/add")
    @PostMapping
    public @ResponseBody
    String cloudAdd(Model model, @RequestParam(name = "pro-image") List<MultipartFile> files,
                    @RequestParam(name = "dir") String dir) throws Exception {
        final String dir64 =
                new String(Base64.getEncoder().encode(dir.getBytes()), StandardCharsets.UTF_8)
                        .replaceAll("/", "")
                        .replaceAll("[-+.^:,]", "");

        for (final MultipartFile multipartFile : files) {
            final String contentType = multipartFile.getContentType().toLowerCase();
            final File f = save(multipartFile.getOriginalFilename(), dir64, multipartFile.getBytes());
            if (contentType.contains("jpg") || contentType.contains("jpeg")) {
                saveCompressedImage(multipartFile.getOriginalFilename(), dir64 + "-web", f);
            }
        }
        return "http://yoursol.store/cloud/" + dir64;
    }

    @RequestMapping("/cloud/download")
    public void download(Model model,
                         @RequestParam("dir") String dir,
                         @RequestParam("name") String name,
                         final HttpServletRequest request,
                         final HttpServletResponse response
    ) throws IOException {
        final File directory = new File("cloud/" + dir);
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.getName().equalsIgnoreCase(name)) {
                    write(response, request, file);
                }
            }
        }
    }

    @RequestMapping("/cloud/download/zip/")
    public void download(Model model,
                         @RequestParam("dir") String dir,
                         final HttpServletResponse response
    ) throws IOException {
        final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"" + dir + ".zip\"");
        File directory = new File("cloud/" + dir);
        if (directory.exists()) {
            final File[] files = directory.listFiles();
            if (files == null) {
                throw new IOException("directory empty");
            }
            for (File file : files) {
                zipFile(file, file.getName(), zipOut);
            }
        }
        zipOut.close();
    }

    @RequestMapping(value = "/cloud/load/image", method = RequestMethod.GET)
    @ResponseBody
    public Object loadImage(Model model,
                            @RequestParam("dir") String dir,
                            @RequestParam("name") String name
    ) throws IOException {
        final File directoryWeb = new File("cloud/" + dir + "-web");
        if (directoryWeb.exists()) {
            for (final File file : Objects.requireNonNull(directoryWeb.listFiles())) {
                if (file.getName().equalsIgnoreCase(name)) {
                    return new String(Base64.getEncoder().encode(readFileToByteArray(file)), StandardCharsets.UTF_8);
                }
            }
        } else {
            final File directory = new File("cloud/" + dir);
            for (final File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.getName().equalsIgnoreCase(name)) {
                    return file;
                }
            }
        }

        return null;
    }

    @RequestMapping(value = "/cloud/{dir}")
    public String watch(Model model, @PathVariable("dir") String dir) throws IOException {
        final List<String> names = new LinkedList<>();
        final File directoryWeb = new File("cloud/" + dir + "-web");
        if (directoryWeb.exists()) {
            final File[] files = directoryWeb.listFiles();
            if (files != null) {
                for (final File file : files) {
                    names.add(file.getName());
                }
            }
        }
        final File directory = new File("cloud/" + dir);
        if (directory.exists()) {
            final File[] files = directory.listFiles();
            if (files != null) {
                for (final File file : files) {
                    final String fileName = file.getName().toLowerCase();
                    if (!(fileName.contains("jpg") || fileName.contains("jpeg")) && fileName.contains(".")) {
                        names.add(file.getName());
                    }
                }
            }
        }
        if (names.size() > 0) {
            model.addAttribute("names", names);
            return "cloudWatch";
        } else {
            return "cloudWatchNotFound";
        }
    }

    @Deprecated
    @RequestMapping(value = "/cloud-carousel/{dir}")
    public String watchCarousel(Model model, @PathVariable("dir") String dir) throws IOException {
        List<String> images = new LinkedList<>();
        List<String> names = new LinkedList<>();
        File directory = new File("cloud/" + dir + "-web");
        if (directory.exists()) {
            final File[] files = directory.listFiles();
            final int last = files.length - 1;
            final String lastName = files[last].getName();
            for (final File file : files) {
                if (images.size() < 4 || lastName.equals(file.getName())) {
                    images.add("data:image/jpeg;base64," + new String(Base64.getEncoder().encode(readFileToByteArray(file)), StandardCharsets.UTF_8));
                }
                names.add(file.getName());
            }
            model.addAttribute("last", last);
            model.addAttribute("lastImageInArray", Math.min(last, 4));
        }
        model.addAttribute("images", images);
        model.addAttribute("names", names);

        return "cloudWatchCarousel";
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children == null) {
                throw new IOException("directory empty");
            }
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[5000];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    private static BufferedImage scaleImage(BufferedImage bufferedImage) {
        final double boundSize = 2048;
        final int origWidth = bufferedImage.getWidth();
        final int origHeight = bufferedImage.getHeight();
        final double scale;
        if (origHeight > origWidth) {
            scale = boundSize / origHeight;
        } else {
            scale = boundSize / origWidth;
        }
        if (scale > 1.0) {
            return (bufferedImage);
        }
        final int scaledWidth = (int) (scale * origWidth);
        final int scaledHeight = (int) (scale * origHeight);
        final Image scaledImage = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        final BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = scaledBI.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        return (scaledBI);
    }

    public static void resize(File inputFile, File outputFIle)
            throws IOException {
        BufferedImage inputImage = ImageIO.read(inputFile);
        ImageIO.write(scaleImage(inputImage), "jpeg", outputFIle);
    }


    private static void saveCompressedImage(String fileName, String dir, File file) throws Exception {
        File f = null;
        try {
            f = new File("cloud");
            boolean b = !f.exists() && f.mkdir() || f.exists();
            if (!b) {
                throw new IOException("no create cloud");
            }
            f = new File("cloud/" + dir);
            b = !f.exists() && f.mkdir() || f.exists();
            if (!b) {
                throw new IOException("no create " + dir);
            }
            f = new File("cloud/" + dir + "/" + fileName);
            boolean exist = f.exists();
            if (!exist && f.createNewFile()) {
                resize(file, f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File save(String fileName, String dir, byte[] bytes) throws Exception {
        OutputStream opStream = null;
        File f = null;
        try {
            f = new File("cloud");
            boolean b = !f.exists() && f.mkdir() || f.exists();
            if (!b) {
                throw new IOException("no create cloud");
            }
            f = new File("cloud/" + dir);
            b = !f.exists() && f.mkdir() || f.exists();
            if (!b) {
                throw new IOException("no create " + dir);
            }
            f = new File("cloud/" + dir + "/" + fileName);
            boolean exist = f.exists();
            if (!exist && f.createNewFile()) {
                opStream = new FileOutputStream(f);
                opStream.write(bytes);
                opStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (opStream != null) opStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return f;
    }

    private static byte[] readFileToByteArray(File file) {
        FileInputStream fis;
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }

    private static void write(
            final HttpServletResponse response,
            final HttpServletRequest request,
            File file
    ) throws IOException {
        final ServletOutputStream outputStream = response.getOutputStream();

        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/force-download");
            response.setContentLength((int) file.length());
            String safariEncodedFileName = file.getName();
            String agent = request.getHeader("User-Agent");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safariEncodedFileName + "\"; filename*=UTF-8''" + safariEncodedFileName);
            InputStream in = new FileInputStream(file);
            byte[] readBuffer = new byte[(int) file.length()];
            int length;
            while ((length = in.read(readBuffer)) != -1) {
                outputStream.write(readBuffer, 0, length);
            }
        } catch (Exception ignored) {
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }
}