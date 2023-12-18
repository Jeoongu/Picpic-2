package com.likelion.picpic.controller;

import com.likelion.picpic.DataNotFoundException;
import com.likelion.picpic.domain.User;
import com.likelion.picpic.dto.ReturnPhotoListDto;
import com.likelion.picpic.service.PhotoService;
import com.likelion.picpic.service.S3Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequestMapping("/photo")
@RequiredArgsConstructor
@RestController
@Api(tags = "Photo",description = "프레임 적용한 사진 관련 로직 작성")
@CrossOrigin(origins = "http://localhost:5173")
public class PhotoController{

    private final S3Service s3Service;
    private final PhotoService photoService;

    @PostMapping
    @ApiOperation(value = "사진 저장 api", notes = "헤더로 토큰, 바디로 이미지 주면돼")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 401, message = "실패")
    })
    public ResponseEntity<?> savePhoto(@RequestPart MultipartFile photo,
                                       Authentication authentication)
            throws IOException{
        String email=authentication.getName();
        String url=s3Service.saveFile("photo",email,photo);
        Long userId=s3Service.getUserId(email);
        photoService.savePhoto(userId,url);
        return ResponseEntity.ok().build();
    }
//@PostMapping
//@ApiOperation(value = "사진 저장 api", notes = "헤더로 토큰, 바디로 이미지 주면돼")
//@ApiResponses({
//        @ApiResponse(code = 200, message = "성공"),
//        @ApiResponse(code = 401, message = "실패")
//})
//public ResponseEntity<?> savePhoto(@RequestPart String base64Photo,
//                                   Authentication authentication)
//        throws IOException{
//    String email=authentication.getName();
//    Long userId=s3Service.getUserId(email);
//    String url=s3Service.saveBase64Image("photo",userId,base64Photo);
//    photoService.savePhoto(userId,url);
//    return ResponseEntity.ok().build();
//}

    @GetMapping
    @ApiOperation(value = "사진 리스트 불러오는 api", notes = "헤더로 토큰 주셔야됩니다")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 401, message = "실패")
    })
    public ResponseEntity<ReturnPhotoListDto> getPhotoList(Authentication authentication)
            throws IOException{
        String email=authentication.getName();
        Long userId=s3Service.getUserId(email);
        ReturnPhotoListDto returnPhotoListDto=new ReturnPhotoListDto();
        return ResponseEntity.ok().body(photoService.getPhotoList(userId));
    }

    @ApiOperation(value = "사진 삭제 api", notes = "헤더로 토큰, 바디로 이미지url")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 401, message = "실패")
    })
    @DeleteMapping
    public ResponseEntity<?> deletePhoto(Authentication authentication,
                                         String url){
        try{
            String email=authentication.getName();
            Long userId=s3Service.getUserId(email);
            photoService.deletePhoto(userId, url);
            return new ResponseEntity<>("삭제에 성공하셨습니다.", HttpStatus.OK);
        } catch (DataNotFoundException e){
            e.printStackTrace();
            return new ResponseEntity<>("삭제에 실패하셨습니다", HttpStatus.BAD_REQUEST);
        }
    }


}
