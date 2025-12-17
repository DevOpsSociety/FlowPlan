package com.hanmo.flowplan.task.application;

import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.task.domain.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TaskStatusPropagator {

  private final TaskRepository taskRepository;

  /**
   * 작업 수정 시 상태 및 진행률 변경에 따른 전파 로직 수행
   */
  @Transactional
  public void propagateUpdates(Task task, TaskStatus oldStatus, Integer oldProgress) {
    boolean isStatusChanged = !oldStatus.equals(task.getStatus());
    boolean isProgressChanged = !Objects.equals(oldProgress, task.getProgress());

    if (isStatusChanged) {
      handleStatusChange(task);
    } else if (isProgressChanged) {
      // 상태는 그대로인데 진행률만 변경된 경우 -> 상위 작업 진행률 재계산만 수행
      if (task.getParent() != null) {
        calculateParentProgress(task.getParent());
      }
    }
  }

  /**
   * 작업 생성/삭제 시 부모 작업의 진행률 재계산 (외부 호출용)
   */
  @Transactional
  public void updateParentProgress(Task parent) {
    if (parent != null) {
      calculateParentProgress(parent);
    }
  }

  // --- Internal Logic ---

  private void handleStatusChange(Task task) {
    TaskStatus currentStatus = task.getStatus();

    if (currentStatus == TaskStatus.DONE) {
      Integer progressObjDone = task.getProgress();
      int progressDone = (progressObjDone == null) ? 0 : progressObjDone;
      if (progressDone < 100) {
        task.setProgress(100);
        taskRepository.save(task);
      }
      // 완료: 하위 작업 모두 완료(100%) 처리, 상위는 계산
      propagateStatusDown(task, TaskStatus.DONE, 100);
      if (task.getParent() != null) {
        calculateParentProgress(task.getParent());
      }
    } else if (currentStatus == TaskStatus.IN_PROGRESS) {
      task.setStatus(TaskStatus.IN_PROGRESS);
      Integer progressObj = task.getProgress();
      int progress = (progressObj == null) ? 0 : progressObj;
      if (progress < 1) {
        task.setProgress(1);
      }
      taskRepository.save(task);
      // 진행 중: 상위/하위 모두 진행 중(1%)으로 강제 변경
      propagateStatusDown(task, TaskStatus.IN_PROGRESS, 1);
      updateAncestorsStatus(task, TaskStatus.IN_PROGRESS);
      if (task.getParent() != null) {
        calculateParentProgress(task.getParent());
      }
    } else if (currentStatus == TaskStatus.TODO) {
      // 할 일: 상위/하위 모두 할 일(0%)로 강제 변경
      propagateStatusDown(task, TaskStatus.TODO, 0);
      propagateStatusUp(task, TaskStatus.TODO, 0);
    } else {
      // 그 외 상태 변경 시 상위 진행률 재계산
      if (task.getParent() != null) {
        calculateParentProgress(task.getParent());
      }
    }
  }

  private void propagateStatusDown(Task parent, TaskStatus status, int progress) {
    List<Task> children = taskRepository.findAllByParentId(parent.getId());

    for (Task child : children) {
      child.setStatus(status);
      child.setProgress(progress);
      propagateStatusDown(child, status, progress);
    }
    taskRepository.saveAll(children);
  }

  private void propagateStatusUp(Task child, TaskStatus status, int progress) {
    Task parent = child.getParent();
    if (parent == null) return;

    parent.setStatus(status);
    parent.setProgress(progress);
    taskRepository.save(parent);

    propagateStatusUp(parent, status, progress);
  }

  private void updateAncestorsStatus(Task child, TaskStatus status) {
    Task parent = child.getParent();
    if (parent == null) return;

    // 이미 해당 상태라면 전파 중단 (최적화)
    if (parent.getStatus() != status) {
      parent.setStatus(status);
      taskRepository.save(parent);
      updateAncestorsStatus(parent, status);
    }
  }

  private void calculateParentProgress(Task parent) {
    List<Task> children = taskRepository.findAllByParentId(parent.getId());

    if (children.isEmpty()) return;

    double sum = children.stream().mapToInt(Task::getProgress).sum();
    int avgProgress = (int) Math.round(sum / children.size());

    parent.updateProgressFromChildren(avgProgress);
    taskRepository.save(parent);

    if (parent.getParent() != null) {
      calculateParentProgress(parent.getParent());
    }
  }
}