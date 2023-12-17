package com.likelion.picpic.service;


import com.likelion.picpic.DataNotFoundException;
import com.likelion.picpic.domain.Memo;
import com.likelion.picpic.domain.PhotoBook;
import com.likelion.picpic.domain.User;
import com.likelion.picpic.dto.MemoCreateDto;
import com.likelion.picpic.repository.MemoRepository;
import com.likelion.picpic.repository.PhotoBookRepository;
import com.likelion.picpic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final PhotoBookRepository photoBookRepository;
    private final UserRepository userRepository;

    public void saveMemo(MemoCreateDto memoCreateDto, String uuid){
        Optional<User> OptUser=userRepository.findByUuid(uuid);
        if(OptUser.isPresent()){
            User user=OptUser.get();

            Long id = user.getPhotoBook().getId();

            Optional<PhotoBook> optionalPhotoBook = photoBookRepository.findById(id);
            if(optionalPhotoBook.isEmpty()) throw new DataNotFoundException("포토북이 존재하지 않습니다.");

            PhotoBook photoBook=optionalPhotoBook.get();
            Memo memo=Memo.from(memoCreateDto, photoBook);

            System.out.println(memoCreateDto.getContent());
            memoRepository.save(memo);
        }
        else throw new DataNotFoundException("유저가 없습니다.");
    }

    public void deleteMemo(MemoCreateDto memoCreateDto){
        int x= memoCreateDto.getX();
        int y= memoCreateDto.getY();
        int pNum= memoCreateDto.getPageNum();
        Optional<Memo> optMemo=memoRepository.findByXAndYAndPageNum(x,y,pNum);
        if(optMemo.isPresent()){
            Memo memo=optMemo.get();
            memoRepository.delete(memo);
        }
        else throw new DataNotFoundException("메모가 없습니다");
    }


}
