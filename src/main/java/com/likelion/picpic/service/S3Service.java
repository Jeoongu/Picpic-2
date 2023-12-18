package com.likelion.picpic.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.likelion.picpic.domain.User;
import com.likelion.picpic.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3 amazonS3;
    private final UserService userService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${jwt.secret}")
    private String secretKey;

    public Long getUserId(String email){
        Long userId=userService.findUserId(email);
        //토큰으로 userId가져오기
        return userId;
    }


    /*
    getURl()을 통해 파일이 저장된 URL을 return 해주고,
    이 URL로 이동 시 해당 파일이 오픈됨(버킷 정책 변경 완료)
     */
    public String saveFile(String directory,String email, MultipartFile multipartFile) throws IOException {
        //TODO: String directory는 Frame이거나 그림일기의 그림이거나 나눠주는 디렉토리
        Long userId=getUserId(email);
        //타임 스탬프로 파일이 계속 덮혀 쓰여지는 것 방지
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String originalFilename = directory+"/"+userId+"/"+timeStamp+"_"+multipartFile.getOriginalFilename();


        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        amazonS3.putObject(bucket, originalFilename, multipartFile.getInputStream(), metadata);
        return amazonS3.getUrl(bucket, originalFilename).toString();
    }

    public String updateFile(String url, MultipartFile multipartFile) throws IOException{

        // 기존 객체 삭제
        if (amazonS3.doesObjectExist(bucket, url)) {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, url));
        }
        // 새로운 객체 업로드
        amazonS3.putObject(new PutObjectRequest(bucket, url, multipartFile.getInputStream(), null));
        return amazonS3.getUrl(bucket, url).toString();

    }
    // 이미지 다운로드, 리턴값 변경 필요한지 찾아봐야함.
    public ResponseEntity<UrlResource> downloadImage(String originalFilename) {
        UrlResource urlResource = new UrlResource(amazonS3.getUrl(bucket, originalFilename));

        String contentDisposition = "attachment; filename=\"" +  originalFilename + "\"";

        // header에 CONTENT_DISPOSITION 설정을 통해 클릭 시 다운로드 진행
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(urlResource);

    }

    public void deleteFrameImage(String email, String url)  {
        Long userId = userService.findUserId(email);

        String originalFilename = extractFilenameFromS3Url(url);
        System.out.println("originalFilename = " + originalFilename);
        System.out.println(originalFilename);
        amazonS3.deleteObject(bucket, "frame/" + userId + "/" + originalFilename);
    }

    private static String extractFilenameFromS3Url(String s3Url) {
        if (s3Url == null || !s3Url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid S3 URL: URL is null or missing proper protocol");
        }

        try {
            URL url = new URL(s3Url);
            String path = url.getPath();
            String[] pathComponents = path.split("/");
            if (pathComponents.length == 0) {
                throw new IllegalArgumentException("Invalid S3 URL: No path found in URL");
            }

            String filename = pathComponents[pathComponents.length - 1];
            // URL 디코딩 후 파일 이름 추출
            return java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid S3 URL: Malformed URL", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid S3 URL: Unsupported Encoding", e);
        }
    }


//    private static String extractFilenameFromS3Url(String s3Url) {
//        try {
//            URL url = new URL(s3Url);
//            String path = url.getPath();
//            String[] pathComponents = path.split("/");
//            String filename = pathComponents[pathComponents.length - 1];
//            // URL 디코딩 후 파일 이름 추출
//            String decodedFilename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
//            return decodedFilename;
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Invalid S3 URL", e);
//        }
//    }

    //userId로 이미지 저장된거 다 가져오기
    public List<String> findImageUrlsByUserId(String email, String directory) {
        Long userId=getUserId(email);
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(directory+"/"+userId + "/");

        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);

        List<String> imageUrls = new ArrayList<>();
        do {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                String imageUrl = amazonS3.getUrl(bucket, objectSummary.getKey()).toString();
                imageUrls.add(imageUrl);
            }

            // S3 객체 목록 요청은 페이지네이션될 수 있으므로, 다음 페이지가 있으면 계속 가져옵니다.
            listObjectsRequest.setMarker(objectListing.getNextMarker());
            objectListing = amazonS3.listObjects(listObjectsRequest);
        } while (objectListing.isTruncated());

        return imageUrls;
    }

    public List<String> findStickerImageUrls(String directory, String theme) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(directory+"/"+theme + "/");

        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);

        List<String> imageUrls = new ArrayList<>();
        do {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                String imageUrl = amazonS3.getUrl(bucket, objectSummary.getKey()).toString();
                imageUrls.add(imageUrl);
            }

            // S3 객체 목록 요청은 페이지네이션될 수 있으므로, 다음 페이지가 있으면 계속 가져옵니다.
            listObjectsRequest.setMarker(objectListing.getNextMarker());
            objectListing = amazonS3.listObjects(listObjectsRequest);
        } while (objectListing.isTruncated());

        return imageUrls;
    }
}

