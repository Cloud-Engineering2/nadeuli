/* S3Service.java
 * nadeuli Service - 여행
 * AWS S3 관련 Service
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : S3Service
 * 이홍비    2025.02.26     s3 에서 파일 삭제 시 cloudfront cache 무효화 처리
 * 이홍비    2025.02.26     extractRelativePathFromUrl() 구현
 * ========================================================
 */

package nadeuli.service;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private final AmazonCloudFront cloudFront;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloudfront.url}") // CloudFront 도메인 설정
    private String cloudFrontUrl;

    @Value("${cloud.aws.cloudfront.distribution-id}")
    private String cloudFrontDistributionId;

    private final String JOURNAL_DIR_NAME = "journal";
    private final String PROFILE_DIR_NAME = "profile";
    private final String ETC_DIR_NAME = "etc";

    // 사진 올리기
    public String uploadFile(MultipartFile file, String kind) {
        System.out.println("🔥 S3 & Cloud Front - 사진 올리기 실행!");

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String s3Key;
            if (kind.equals("profile")) {
                s3Key = PROFILE_DIR_NAME + "/" + fileName;
            }
            else if (kind.equals("journal")) {
                s3Key = JOURNAL_DIR_NAME + "/" + fileName;
            }
            else {
                s3Key = ETC_DIR_NAME + "/" + fileName;
            }


            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // S3에 파일 업로드
//            amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, file.getInputStream(), metadata)
//                    .withCannedAcl(CannedAccessControlList.PublicRead));

            amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, file.getInputStream(), metadata));


            // CloudFront URL 반환
            return cloudFrontUrl + "/" + s3Key;
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    // 파일 삭제 + 캐시 무효화
    public void deleteFile(String fileName, String kind) {

        String s3Key = extractRelativePathFromUrl(fileName);

        // Cloud Front - 캐시 무효화
        // System.out.println("🔥 캐시 무효화 : " + fileName);
        // invalidateCache(s3Key, kind);

        // S3 - 사진 삭제
        System.out.println("🔥 사진 삭제 : " + s3Key);
        amazonS3.deleteObject(bucketName, s3Key);
    }

    // Cloud Front - 캐시 무효화
//    private void invalidateCache(String fileName, String kind) {
//        Paths paths = new Paths()
//                .withItems("/" + fileName)
//                .withQuantity(1);  // 경로 개수 명시

        // 🔥 CloudFront URL 제거 후 상대 경로 추출
//        String relativePath = fileName.replace(cloudFrontUrl, "");  // 도메인 제거
//        if (!relativePath.startsWith("/")) {
//            relativePath = "/" + relativePath;  // 앞에 '/' 추가
//        }
//
//        String relativePath = "/" + fileName;
//
//        Paths paths = new Paths()
//                .withItems(relativePath)
//                .withQuantity(1);  // 경로 개수 명시
//
//
//        System.out.println("🔥 캐시 무효화 - paths : " + paths);
//        System.out.println("🔥 캐시 무효화 - relativePath : " + relativePath);
//
//        InvalidationBatch invalidationBatch = new InvalidationBatch()
//                .withPaths(paths)  // 경로 설정
//                .withCallerReference(UUID.randomUUID().toString());  // 고유한 요청 ID
//
//        CreateInvalidationRequest request = new CreateInvalidationRequest()
//                .withDistributionId(cloudFrontDistributionId)
//                .withInvalidationBatch(invalidationBatch);
//
//        cloudFront.createInvalidation(request);  // 캐시 무효화 요청
//    }

    // DB 에 저장된 image URL => s3 에 저장된 파일 이름 추출 함수
    private String extractRelativePathFromUrl(String imageUrl) {
        // CloudFront URL 기준 - 경로 추출
        String relativePath = imageUrl.replace(cloudFrontUrl, "");

        // 앞에 '/' 있으면 삭제
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1); // 첫 번째 '/' 제거
        }

        return relativePath;
    }

}
