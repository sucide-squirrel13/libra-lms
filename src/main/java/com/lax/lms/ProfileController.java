package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class ProfileController {

    private static final long MAX_IMAGE_SIZE = 3 * 1024 * 1024;
    private static final Path DEFAULT_UPLOAD_DIRECTORY =
            Paths.get("uploads", "profiles").toAbsolutePath().normalize();

    private final LibraryUserRepository userRepository;
    private Path uploadDirectory;

    @Autowired
    public ProfileController(LibraryUserRepository userRepository) {
        this(userRepository, DEFAULT_UPLOAD_DIRECTORY);
    }

    public ProfileController(LibraryUserRepository userRepository, Path uploadDirectory) {
        this.userRepository = userRepository;
        this.uploadDirectory = uploadDirectory != null
                ? uploadDirectory.toAbsolutePath().normalize()
                : DEFAULT_UPLOAD_DIRECTORY;
    }

    public void setUploadDirectory(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory != null
                ? uploadDirectory.toAbsolutePath().normalize()
                : DEFAULT_UPLOAD_DIRECTORY;
    }

    @GetMapping("/profile/photo")
    public ResponseEntity<byte[]> getProfilePhoto(
            Authentication authentication) {

        String email = AuthenticatedEmailResolver.resolve(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userOptional =
                userRepository.findByEmailIgnoreCase(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Long userId = userOptional.get().getId();

        for (String extension : new String[]{".png", ".jpg"}) {
            Path imagePath = uploadDirectory.resolve(userId + extension);

            try {
                if (Files.isRegularFile(imagePath)) {
                    byte[] image = Files.readAllBytes(imagePath);

                    MediaType mediaType = extension.equals(".png")
                            ? MediaType.IMAGE_PNG
                            : MediaType.IMAGE_JPEG;

                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .cacheControl(CacheControl.noCache())
                            .body(image);
                }
            } catch (IOException exception) {
                return ResponseEntity.internalServerError().build();
            }
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/profile/photo")
    public String uploadProfilePhoto(
            @RequestParam("photo") MultipartFile photo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = AuthenticatedEmailResolver.resolve(authentication);

        if (email == null) {
            return "redirect:/login";
        }

        if (photo == null || photo.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "profileError", "Please choose a picture first.");
            return "redirect:/user/dashboard";
        }

        if (photo.getSize() > MAX_IMAGE_SIZE) {
            redirectAttributes.addFlashAttribute(
                    "profileError", "Your picture must be smaller than 3 MB.");
            return "redirect:/user/dashboard";
        }

        try {
            String contentType = photo.getContentType();

            String extension;

            if ("image/png".equalsIgnoreCase(contentType)) {
                extension = ".png";
            } else if ("image/jpeg".equalsIgnoreCase(contentType)) {
                extension = ".jpg";
            } else {
                redirectAttributes.addFlashAttribute(
                        "profileError",
                        "Please upload a PNG or JPEG image.");
                return "redirect:/user/dashboard";
            }

            // Decode the uploaded image to reject invalid image files.
            BufferedImage decodedImage = ImageIO.read(photo.getInputStream());

            if (decodedImage == null
                    || decodedImage.getWidth() <= 0
                    || decodedImage.getHeight() <= 0
                    || decodedImage.getWidth() > 6000
                    || decodedImage.getHeight() > 6000) {

                redirectAttributes.addFlashAttribute(
                        "profileError",
                        "The image is invalid or its dimensions are too large.");

                return "redirect:/user/dashboard";
            }

            Optional<LibraryUser> userOptional =
                    userRepository.findByEmailIgnoreCase(email);

            if (userOptional.isEmpty()) {
                return "redirect:/login";
            }

            Long userId = userOptional.get().getId();

            Files.createDirectories(uploadDirectory);

            // Remove any previous profile picture for this user.
            Files.deleteIfExists(uploadDirectory.resolve(userId + ".png"));
            Files.deleteIfExists(uploadDirectory.resolve(userId + ".jpg"));

            Path destination =
                    uploadDirectory.resolve(userId + extension).normalize();

            if (!destination.getParent().equals(uploadDirectory)) {
                redirectAttributes.addFlashAttribute(
                        "profileError", "Invalid image destination.");
                return "redirect:/user/dashboard";
            }

            Files.write(destination, photo.getBytes());

            redirectAttributes.addFlashAttribute(
                    "profileSuccess", "Profile picture updated successfully!");

        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute(
                    "profileError",
                    "Could not save your picture. Please try again.");
        }

        return "redirect:/user/dashboard";
    }
}