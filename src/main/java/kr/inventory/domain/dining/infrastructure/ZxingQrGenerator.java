package kr.inventory.domain.dining.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import kr.inventory.domain.dining.exception.QrErrorCode;
import kr.inventory.domain.dining.exception.QrException;
import kr.inventory.domain.dining.service.QrGenerator;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

@Component
public class ZxingQrGenerator implements QrGenerator {

    @Override
    public byte[] generate(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new QrException(QrErrorCode.QR_IMAGE_GENERATION_FAILED);
        }
    }
}