package com.edutech.team26.controller;

import com.edutech.team26.biz.CategoryService;
import com.edutech.team26.biz.LectureService;
import com.edutech.team26.biz.TeacherService;
import com.edutech.team26.domain.Lecture;
import com.edutech.team26.domain.VwCourse;
import com.edutech.team26.domain.VwLecture;
import com.edutech.team26.domain.VwTeacher;
import com.edutech.team26.dto.LectureDTO;
import com.edutech.team26.model.LectureParam;
import com.edutech.team26.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/admin")
public class AdminController extends lecBaseController {
    @Autowired
    private ModelMapper mapper;

    @Autowired
    private LectureService lectureService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private VwTeacherRepository vwTeacherRepository;

    @Autowired
    private VwCourseRepository vwCourseRepository;
    @Autowired
    private VwLectureRepository vwLectureRepository;


    /*@PreAuthorize("hasRole('ADMIN')")*/
    @GetMapping("/home")
    public String adminHome(Model model) {
        return "admin/home";
    }


    // ===========================================   [강의 관련 시작]  ============================================================

    //셀렉트박스 AJAX
    @GetMapping("/filteredLectureList")
    public String filteredLectureList(Model model, @RequestParam(name = "status", defaultValue = "0") int status) {
        System.out.println("Status: " + status);
        List<VwLecture> filteredList;

        if (status == 0) {
            // Return all lectures if status is 0 or not provided
            filteredList = vwLectureRepository.findAllByOrderByLecRegDateDesc();
        } else {
            // Otherwise, filter by status
            filteredList = vwLectureRepository.findByLectureAct(status);
        }
        System.out.println("상태별 >>>>>>>>>> " + filteredList);
        model.addAttribute("lectureList", filteredList);

        // Return only the fragment (table)
        return "admin/lecture/lectureList :: #lectureTableFragment";
    }



    // 개설 강좌 전체 리스트
    @GetMapping("/lectureList")
    public String lecturelist(Model model, @RequestParam(name = "status", defaultValue = "0") int status, LectureParam lectureParam) {

//        lectureParam.init();
//        List<lectureDTO> lectureList = lectureService.list(lectureParam);

//        long totalCount = 0;
//        if (!CollectionUtils.isEmpty(lectureList)) {
//            totalCount = lectureList.get(0).getTotalCount();
//        }
//
//        String queryString = lectureParam.getQueryString();
//        String pageHtml  = getPaperHtml(totalCount,
//                lectureParam.getPageSize(),
//                lectureParam.getPageIndex(),
//                queryString);
//        model.addAttribute("totalCount", totalCount);
//        model.addAttribute("pager", pageHtml);

//        Lecture lecture = new Lecture();
//        lecture.setStartStudyDate(lecture.getStartStudyDate());
//        lecture.setEndStudyDate(lecture.getEndStudyDate());
//        lecture.updateLectureAct();
/*
        List<Lecture> lecList =  lectureRepository.findAll();
        System.out.println("강의 >>>>>" + lecList);

       */



        List<VwLecture> lectureList = vwLectureRepository.findAllByOrderByLecRegDateDesc();
        //List<VwLecture> lectureList = vwLectureRepository.findAll();
        System.out.println("전체 강좌" + lectureList.toString());
        model.addAttribute("lectureList", lectureList);

        return "admin/lecture/lectureList";
    }





    // 선택한 강의 상세보기
    @GetMapping("/getlecture/{lecture_no}")
    public String getProduct(@PathVariable("lecture_no") long lecture_no, Model model) {

        //강의 정보
        VwLecture lecture = vwLectureRepository.getById(lecture_no);
        System.out.println(lecture);


        model.addAttribute("lecture", lecture);
        return "admin/lecture/getlecture";
    }


    // add form
    @GetMapping("/lectureSave")
    public String addForm(Model model) {

//        List<Vwlecture> Vwlectures = new ArrayList<>();


//        List<String> memberNames = new ArrayList<>();
//        for (Teacher teacher : teacherList) {
//            Long mno = teacher.getMno();
//            Member member = memberRepository.findByMno(mno);
//            if (member != null) {
//                String memberName = member.getUserName();
//                memberNames.add(memberName);
//            } else {
//                memberNames.add("Unknown Member");
//            }
//        }
        /*List<Teacher> teacherList = teacherRepository.findAll();
        System.out.println("강사리스트 : " + teacherList);*/

        List<VwLecture> lectureList = vwLectureRepository.findAllByOrderByLecRegDateDesc(); //최신등록순
        //List<VwLecture> lectureList = vwLectureRepository.findAll();
        model.addAttribute("lectureList", lectureList);

        List<VwTeacher> teacherList = vwTeacherRepository.findAll();
        System.out.println("강사리스트 : " + teacherList);

        model.addAttribute("teacherList", teacherList); //선생님 이름 담은 객체
        model.addAttribute("category", categoryService.list());
        return "admin/lecture/addlecture";
    }


    // 강의 이름 중복 확인
    @ResponseBody
    @PostMapping("/checkDuplicate")
    public Map<String, Boolean> checkDuplicateLectureName(@RequestParam("lectureName") String lectureName) {
        // Assuming lectureService.isLectureNameDuplicate returns a boolean indicating whether the name is duplicate
        boolean isDuplicate = lectureService.isLectureNameDuplicate(lectureName);

        // Prepare a JSON response
        Map<String, Boolean> response = new HashMap<>();
        response.put("duplicate", isDuplicate);

        return response;
    }


    //강의 등록하기
    @PostMapping(value = {"/save"})
    public String saveSubmit(Model model, HttpServletRequest request, MultipartFile[] file, LectureDTO lectureDTO) throws IOException {


        lectureDTO = updateLectureAct(lectureDTO);
        lectureService.addLecture(lectureDTO, file, request);
        return "redirect:/admin/lectureList";
    }


    // 강의 상태 변경 (Entity에서 가져올때)
    public LectureDTO updateLectureAct(LectureDTO lectureDTO) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Parse the start and end study dates
        LocalDate startStudyLocalDate = LocalDate.parse(lectureDTO.getStartStudyDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endStudyLocalDate = LocalDate.parse(lectureDTO.getEndStudyDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // Create a new Lecture entity and set lectureAct based on the conditions
        if (startStudyLocalDate.isAfter(currentDate)) {
            lectureDTO.setLectureAct(1); // 강의예정
        } else if (startStudyLocalDate.isBefore(currentDate) && endStudyLocalDate.isAfter(currentDate)) {
            lectureDTO.setLectureAct(2); // 강의진행중
        } else if (endStudyLocalDate.isBefore(currentDate)) {
            lectureDTO.setLectureAct(3); // 강의종료
        }

        // Optionally, you might want to persist the changes to the database
        // Uncomment the following line if you are using Spring Data JPA
        // lectureRepository.save(lecture);

        return lectureDTO;
    }


    //강의 수정 Form 이동
    @GetMapping(value = {"/updateLecture/{lecture_no}"})
    public String updateForm(Model model, HttpServletRequest request, @PathVariable("lecture_no") long lecture_no) {

        //강의 정보
        VwLecture lecture = vwLectureRepository.getById(lecture_no);
        System.out.println(lecture);
        model.addAttribute("lecture", lecture);

        //선생님 정보
        List<VwTeacher> teacherList = vwTeacherRepository.findAll();
        System.out.println("강사리스트 : " + teacherList);
        model.addAttribute("teacherList", teacherList); //선생님 이름 담은 객체

        return "admin/lecture/updatelecture";
    }

    // 강의 수정 처리
    @PostMapping(value = {"/updateLecture/{lecture_no}"})
    public String updateSubmit(Model model, @PathVariable("lecture_no") long lecture_no, MultipartFile[] file, LectureDTO lectureDTO) throws IOException {

        lectureDTO = updateLectureAct(lectureDTO);
        lectureService.updateLecture(lectureDTO, lecture_no, file);
        return "redirect:/admin/lectureList";
    }


    //강의 철회하기(삭제 x)
    @GetMapping("/deleteLecture/{lecture_no}")
    public String deleteLecture(@PathVariable("lecture_no") long lecture_no, Model model) {

        lectureService.deleteLecture(lecture_no);

        return "redirect:/admin/lectureList";

    }

    //강의 철회 취소하기
    @GetMapping("/deleteCancleLecture/{lecture_no}")
    public String deleteCancleLecture(@PathVariable("lecture_no") long lecture_no, Model model) {

        lectureService.deleteCancleLecture(lecture_no);

        return "redirect:/admin/lectureList";

    }


    // ===========================================   [강의 관련 끝]  ============================================================


    // ===========================================   [수강생 관련 시작]  ============================================================

    @GetMapping("/courseList")
    public String courseList(Model model, LectureParam lectureParam) {

        List<VwCourse> courseList = vwCourseRepository.findAll();

        System.out.println("전체 강좌" + courseList.toString());
        model.addAttribute("courseList", courseList);

        return "admin/lecture/courseList";
    }

    // ===========================================   [수강생 관련 끝]  ============================================================

}