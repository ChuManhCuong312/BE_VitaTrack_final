package com.vitatrack.integration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload an image file and return its public URL.
     */
    public String uploadImage(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "vitatrack/" + folder,
                            "resource_type", "image",
                            "transformation", "q_auto,f_auto,w_800,c_limit"
                    )
            );
            return result.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Cloudinary upload error: {}", e.getMessage());
            throw new RuntimeException("Không thể upload ảnh: " + e.getMessage());
        }
    }

    /**
     * Upload raw bytes (e.g. from AI analysis pipeline).
     */
    public String uploadBytes(byte[] bytes, String folder, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "vitatrack/" + folder,
                            "public_id", publicId,
                            "overwrite", true
                    )
            );
            return result.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Cloudinary upload bytes error: {}", e.getMessage());
            throw new RuntimeException("Không thể upload ảnh");
        }
    }

    /**
     * Delete an asset by its public_id.
     */
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete error: {}", e.getMessage());
        }
    }
}
