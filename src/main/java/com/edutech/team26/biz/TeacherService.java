package com.edutech.team26.biz;

import com.edutech.team26.domain.VwLecture;
import com.edutech.team26.dto.TeacherHistoryFilesVO;
import com.edutech.team26.dto.TeacherVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherService {

    List<TeacherHistoryFilesVO> getHistoryList(Long mno) throws Exception;

    boolean applyGrade(Long mno, List<MultipartFile> uploadFiles, HttpServletRequest request) throws Exception;

    boolean changeActive(Long teacherNo, int type) throws Exception;

    List<TeacherVO> teacherList();


/*    List<vwlecture> findAll();*/


    VwLecture getByMno(long mno);

}