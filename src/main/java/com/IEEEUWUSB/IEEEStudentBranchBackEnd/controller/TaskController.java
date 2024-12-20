package com.IEEEUWUSB.IEEEStudentBranchBackEnd.controller;

import com.IEEEUWUSB.IEEEStudentBranchBackEnd.dto.AssignTaskDTO;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.dto.CommonResponseDTO;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.dto.StatusCountDTO;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.dto.TaskCreateDTO;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.entity.*;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("api/v1/task")
public class TaskController {

    @Autowired
    private final TaksService taskService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRoleDetailsServices userRoleDetailsServices;

    @Autowired
    private OUService ouService;
    @Autowired
    private UserService userService;


    public TaskController(TaksService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<CommonResponseDTO> addTask(HttpServletRequest request, @RequestBody TaskCreateDTO task) {
        CommonResponseDTO<Task> commonResponseDTO = new CommonResponseDTO<>();
        boolean priorityValidation = task.getPriority().equals("HIGH") || task.getPriority().equals("MEDIUM") || task.getPriority().equals("LOW");
        boolean typeValidation = task.getType().equals("EXCOM") || task.getType().equals("PROJECT");
        User user = (User) request.getAttribute("user");
        List<UserRoleDetails> userRoleDetails = userRoleDetailsServices.getuserRoleDetails(user, true, "MAIN");
        boolean isTaskMainAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "PROJECT");
        if (typeValidation && priorityValidation) {
            try {
                boolean isPolicyAvailable = false;
                Project project;
                OU ou;
                Task newTask = Task.builder()
                        .start_date(task.getStart_date())
                        .end_date(task.getEnd_date())
                        .task_name(task.getTask_name())
                        .type(task.getType())
                        .priority(task.getPriority())
                        .status("TODO")
                        .createdBy(user)
                        .build();
                if (task.getType().equals("EXCOM")) {
                    ou = ouService.getOUById(task.getOu_id());
                    List<UserRoleDetails> userRoleDetailsExcom = userRoleDetailsServices.getuserRoleDetails(user, true, "EXCOM");
                    isPolicyAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetailsExcom, "EXCOM_TASK");
                    newTask.setOu(ou);
                } else if (task.getType().equals("PROJECT")) {
                    project = projectService.getProjectById(task.getProject_id());
                    List<UserRoleDetails> userRoleDetailsProject = userRoleDetailsServices.getuserroledetailsbyuserandproject(user, true, project);
                    isPolicyAvailable = isTaskMainAvailable || userRoleDetailsServices.isPolicyAvailable(userRoleDetailsProject, "PROJECT_TASK");
                    newTask.setProject(project);
                }

                if (isPolicyAvailable) {
                    Task newTaskSaved = taskService.saveTask(newTask);
                    commonResponseDTO.setData(newTaskSaved);
                    commonResponseDTO.setMessage("Task Added Successfully");
                    return new ResponseEntity<>(commonResponseDTO, HttpStatus.CREATED);
                } else {
                    commonResponseDTO.setMessage("No Authority to Add Task");
                    return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                commonResponseDTO.setMessage("Task Added Failed");
                commonResponseDTO.setError(e.getMessage());
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
            }
        } else {
            commonResponseDTO.setMessage(typeValidation ? "Invalid Task Type" : "Invalid Task Priority");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{ouID}")
    public ResponseEntity<CommonResponseDTO> getTask(
            HttpServletRequest request,
            @PathVariable int ouID,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer user_id,
            @RequestParam(required = false, defaultValue = "0") Integer page
    ) {
        CommonResponseDTO<Page<Task>> commonResponseDTO = new CommonResponseDTO<>();
        User user = (User) request.getAttribute("user");
        List<UserRoleDetails> userRoleDetails = userRoleDetailsServices.getuserRoleDetails(user, true, "MAIN");
        boolean isTaskPolicy = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "EXCOM_TASK");

        try {
            Page<Task> tasks;
            OU ou = ouService.getOUById(ouID);
            if (ou == null) {
                commonResponseDTO.setMessage("OU not found");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.NOT_FOUND);
            }
            User assignedUser = null;
            if (user_id != null) {
                assignedUser = userService.getUserId(user_id);
            }
            if (isTaskPolicy) {
                tasks = taskService.findAllTasksByOU(priority, search, ou, status, assignedUser, assignedUser, page);
            } else {
                tasks = taskService.findAllTasksByOU(priority, search, ou, status, user, user, page);
            }
            commonResponseDTO.setData(tasks);
            commonResponseDTO.setMessage("Task retrieved Successfully");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
        } catch (Exception e) {
            commonResponseDTO.setError(e.getMessage());
            commonResponseDTO.setMessage("Task retrieval Failed");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("task/{task_id}")
    public ResponseEntity<CommonResponseDTO> getTaskById(
            HttpServletRequest request,
            @PathVariable int task_id
    ) {
        CommonResponseDTO<Task> commonResponseDTO = new CommonResponseDTO<>();

        try {
            Task task = taskService.getTaskById(task_id);
            if (task == null) {
                commonResponseDTO.setMessage("Task Not Found");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.NOT_FOUND);
            }
            commonResponseDTO.setData(task);
            commonResponseDTO.setMessage("Task retrieved Successfully");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
        } catch (Exception e) {
            commonResponseDTO.setError(e.getMessage());
            commonResponseDTO.setMessage("Task retrieval Failed");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("project/{project_id}")
    public ResponseEntity<CommonResponseDTO> getTaskByProject(
            HttpServletRequest request, @PathVariable int project_id,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer user_id,
            @RequestParam(required = false, defaultValue = "0") Integer page
    ) {
        CommonResponseDTO<Page<Task>> commonResponseDTO = new CommonResponseDTO<>();
        User user = (User) request.getAttribute("user");
        List<UserRoleDetails> userRoleDetails = userRoleDetailsServices.getuserRoleDetails(user, true, "MAIN");
        boolean isTaskMainAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "PROJECT");

        try {
            Page<Task> tasks;
            Project project = projectService.getProjectById(project_id);
            if (project == null) {
                commonResponseDTO.setMessage("Project not found");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.NOT_FOUND);
            }
            User assigneduser = null;
            if (user_id != null) {
                assigneduser = userService.getUserId(user_id);
            }

            List<UserRoleDetails> ProjectuserRoleDetails = userRoleDetailsServices.getuserRoleDetailsByProject(user, true, "PROJECT", project_id);
            boolean isTaskProjectMainAvailable = userRoleDetailsServices.isPolicyAvailable(ProjectuserRoleDetails, "PROJECT_TASK");
            if (isTaskMainAvailable || isTaskProjectMainAvailable) {
                tasks = taskService.findAllTasksByProject(priority, search, project, status, assigneduser, assigneduser, page);
            } else {
                tasks = taskService.findAllTasksByProject(priority, search, project, status, user, user, page);
            }
            commonResponseDTO.setData(tasks);
            commonResponseDTO.setMessage("Task retrieved Successfully");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
        } catch (Exception e) {
            commonResponseDTO.setError(e.getMessage());
            commonResponseDTO.setMessage("Task retrieval Failed");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<CommonResponseDTO> assignUser(HttpServletRequest request, @RequestBody AssignTaskDTO assignTaskDTO) {
        CommonResponseDTO<Task> commonResponseDTO = new CommonResponseDTO<>();
        User user = (User) request.getAttribute("user");
        try {
            List<UserRoleDetails> userRoleDetailsMain = userRoleDetailsServices.getuserRoleDetails(user, true, "MAIN");
            boolean isTaskMainAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetailsMain, "PROJECT");
            String taskType = taskService.getTaskById(assignTaskDTO.getTaskId()).getType();
            List<UserRoleDetails> userRoleDetails = null;
            boolean isTaskPolicyAvailable = false;

            if (taskType.equals("EXCOM")) {
                userRoleDetails = userRoleDetailsServices.getuserRoleDetails(user, true, "EXCOM");
                isTaskPolicyAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "EXCOM_TASK_ASSIGN");
            } else if (taskType.equals("PROJECT")) {
                userRoleDetails = userRoleDetailsServices.getuserRoleDetailsByProject(user, true, "PROJECT", assignTaskDTO.getProject_id());
                isTaskPolicyAvailable = isTaskMainAvailable || userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "PROJECT_TASK");
            } else {
                commonResponseDTO.setMessage("Invalid Type");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.NOT_FOUND);
            }

            if (isTaskPolicyAvailable) {

                String message = taskService.assign(assignTaskDTO.getTaskId(), assignTaskDTO.getUsers());
                commonResponseDTO.setMessage(message);
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);

            } else {
                commonResponseDTO.setMessage("No Authority to Assign Task");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            commonResponseDTO.setMessage(e.getMessage());
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }

    }

    @PutMapping("/status/{taskId}")
    public ResponseEntity<CommonResponseDTO> setStatus(HttpServletRequest request, @PathVariable int taskId, @RequestParam(required = true) String status) {
        CommonResponseDTO<Task> commonResponseDTO = new CommonResponseDTO<>();
        User user = (User) request.getAttribute("user");

        // Validate the status
        boolean statusValidation = status.equals("TODO") || status.equals("PROGRESS") || status.equals("COMPLETE");

        if (!statusValidation) {
            commonResponseDTO.setMessage("Invalid Task Status");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get the task object by the ID
            Task task = taskService.getTaskById(taskId);
            task.setStatus(status);

            // Save the task
            Task savedTask = taskService.saveTask(task);
            commonResponseDTO.setData(savedTask);
            commonResponseDTO.setMessage("Task status updated successfully");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
        } catch (Exception e) {
            commonResponseDTO.setMessage("Task status update failed");
            commonResponseDTO.setError(e.getMessage());
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<CommonResponseDTO> editTask(HttpServletRequest request,
                                                      @PathVariable int taskId,
                                                      @RequestBody TaskCreateDTO taskDTO) {
        CommonResponseDTO<Task> commonResponseDTO = new CommonResponseDTO<>();

        // Retrieve the user from the request
        User user = (User) request.getAttribute("user");

        // Priority and type validation
        boolean priorityValidation = taskDTO.getPriority().equals("HIGH") ||
                taskDTO.getPriority().equals("MEDIUM") ||
                taskDTO.getPriority().equals("LOW");

        boolean statusValidation = taskDTO.getStatus().equals("TODO") ||
                taskDTO.getStatus().equals("PROGRESS") ||
                taskDTO.getStatus().equals("COMPLETE");

        if (!priorityValidation || !statusValidation) {
            commonResponseDTO.setMessage("Invalid task priority or status");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }

        try {
            // Retrieve the task by ID
            Task task = taskService.findTaskById(taskId);

            if (task == null) {
                commonResponseDTO.setMessage("Task not found");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.NOT_FOUND);
            }

            task.setTask_name(taskDTO.getTask_name());
            task.setPriority(taskDTO.getPriority());
            task.setStart_date(taskDTO.getStart_date());
            task.setEnd_date(taskDTO.getEnd_date());
            task.setStatus(taskDTO.getStatus());
            task.setDescription(taskDTO.getDescription());
            Task updatedTask = taskService.saveTask(task);

            commonResponseDTO.setData(updatedTask);
            commonResponseDTO.setMessage("Task updated successfully");
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);

        } catch (Exception e) {
            commonResponseDTO.setMessage("Failed to update task");
            commonResponseDTO.setError(e.getMessage());
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<CommonResponseDTO> getTaskCount(HttpServletRequest request,
                                                          @RequestParam(required = true) String taskType,
                                                          @RequestParam(required = false) Integer projectId,
                                                          @RequestParam(required = false) Integer ouId) {

        CommonResponseDTO<StatusCountDTO> commonResponseDTO = new CommonResponseDTO<>();
        User user = (User) request.getAttribute("user");
        OU ou = (ouId != null) ? ouService.getOUById(ouId) : null;
        Project project = (projectId != null) ? projectService.getProjectById(projectId) : null;


        List<UserRoleDetails> userRoleDetails = userRoleDetailsServices.getuserRoleDetails(user, true, "MAIN");
        boolean isExcomPolicyAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "EXCOM");
        boolean isMainProjectPolicyAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails, "PROJECT");

        List<UserRoleDetails> userRoleDetails1 = userRoleDetailsServices.getuserRoleDetailsByProject(user, true, "PROJECT", projectId);
        boolean isProjectPolicyAvailable = userRoleDetailsServices.isPolicyAvailable(userRoleDetails1, "PROJECT_TASK");



        try {
            StatusCountDTO data = null;
            if (taskType.equals("EXCOM") && isExcomPolicyAvailable && ouId != null) {
                long todo = taskService.countTaskByOu(ou, "TODO");
                long progress = taskService.countTaskByOu(ou, "PROGRESS");
                long complete = taskService.countTaskByOu(ou, "COMPLETE");
                data = new StatusCountDTO(todo, progress, complete);
                commonResponseDTO.setData(data);
                commonResponseDTO.setMessage("Successfully EXCOM Tasks Retrieved");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);

            } else if (taskType.equals("PROJECT") && (isProjectPolicyAvailable || isMainProjectPolicyAvailable) && projectId != null) {
                long todo = taskService.countAllTaskByProject(project, "TODO");
                long progress = taskService.countAllTaskByProject(project, "PROGRESS");
                long complete = taskService.countAllTaskByProject(project, "COMPLETE");
                data = new StatusCountDTO(todo, progress, complete);
                commonResponseDTO.setData(data);
                commonResponseDTO.setMessage("Successfully ALL Project Tasks Retrieved");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
            } else if (taskType.equals("PROJECT")  && projectId != null) {
                long todo = taskService.countTaskByProjectWithUser(project, user, "TODO");
                long progress = taskService.countTaskByProjectWithUser(project, user, "PROGRESS");
                long complete = taskService.countTaskByProjectWithUser(project, user, "COMPLETE");
                data = new StatusCountDTO(todo, progress, complete);
                commonResponseDTO.setData(data);
                commonResponseDTO.setMessage("Successfully Project Tasks Retrieved");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.OK);
            }else{
                commonResponseDTO.setMessage("Invalid Task Type");
                return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            commonResponseDTO.setMessage("Failed to get task count");
            commonResponseDTO.setError(e.getMessage());
            return new ResponseEntity<>(commonResponseDTO, HttpStatus.BAD_REQUEST);
        }

    }
}
