package io.metersphere.track.service;

import io.metersphere.base.domain.TestCaseComment;
import io.metersphere.base.domain.TestCaseCommentExample;
import io.metersphere.base.domain.TestCaseWithBLOBs;
import io.metersphere.base.domain.User;
import io.metersphere.base.mapper.TestCaseCommentMapper;
import io.metersphere.base.mapper.TestCaseMapper;
import io.metersphere.base.mapper.UserMapper;
import io.metersphere.base.mapper.ext.ExtTestCaseCommentMapper;
import io.metersphere.commons.constants.NoticeConstants;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.BaseSystemConfigDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.notice.sender.NoticeModel;
import io.metersphere.notice.service.NoticeSendService;
import io.metersphere.service.SystemParameterService;
import io.metersphere.track.dto.TestCaseCommentDTO;
import io.metersphere.track.request.testreview.SaveCommentRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestCaseCommentService {

    @Resource
    private TestCaseCommentMapper testCaseCommentMapper;
    @Resource
    private TestCaseMapper testCaseMapper;
    @Resource
    private ExtTestCaseCommentMapper extTestCaseCommentMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoticeSendService noticeSendService;
    @Resource
    private SystemParameterService systemParameterService;

    public void saveComment(SaveCommentRequest request) {
        TestCaseComment testCaseComment = new TestCaseComment();
        testCaseComment.setId(UUID.randomUUID().toString());
        testCaseComment.setAuthor(SessionUtils.getUser().getId());
        testCaseComment.setCaseId(request.getCaseId());
        testCaseComment.setCreateTime(System.currentTimeMillis());
        testCaseComment.setUpdateTime(System.currentTimeMillis());
        testCaseComment.setDescription(request.getDescription());
        testCaseCommentMapper.insert(testCaseComment);
        TestCaseWithBLOBs testCaseWithBLOBs;
        testCaseWithBLOBs = testCaseMapper.selectByPrimaryKey(request.getCaseId());

        // ????????????
        User user = userMapper.selectByPrimaryKey(testCaseWithBLOBs.getMaintainer());
        BaseSystemConfigDTO baseSystemConfigDTO = systemParameterService.getBaseInfo();
        List<String> userIds = new ArrayList<>();
        userIds.add(testCaseWithBLOBs.getMaintainer());//???????????????
        String context = getReviewContext(testCaseComment, testCaseWithBLOBs);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("maintainer", user.getName());
        paramMap.put("testCaseName", testCaseWithBLOBs.getName());
        paramMap.put("description", request.getDescription());
        paramMap.put("url", baseSystemConfigDTO.getUrl());
        paramMap.put("id", request.getReviewId());
        NoticeModel noticeModel = NoticeModel.builder()
                .context(context)
                .relatedUsers(userIds)
                .subject(Translator.get("test_review_task_notice"))
                .mailTemplate("ReviewComments")
                .paramMap(paramMap)
                .event(NoticeConstants.Event.COMMENT)
                .build();
        noticeSendService.send(NoticeConstants.TaskType.REVIEW_TASK, noticeModel);
    }

    public List<TestCaseCommentDTO> getCaseComments(String caseId) {
        return extTestCaseCommentMapper.getCaseComments(caseId);
    }

    public void deleteCaseComment(String caseId) {
        TestCaseCommentExample testCaseCommentExample = new TestCaseCommentExample();
        testCaseCommentExample.createCriteria().andCaseIdEqualTo(caseId);
        testCaseCommentMapper.deleteByExample(testCaseCommentExample);
    }

    private String getReviewContext(TestCaseComment testCaseComment, TestCaseWithBLOBs testCaseWithBLOBs) {
        User user = userMapper.selectByPrimaryKey(testCaseComment.getAuthor());
        Long startTime = testCaseComment.getCreateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String start = null;
        String sTime = String.valueOf(startTime);
        if (!sTime.equals("null")) {
            start = sdf.format(new Date(Long.parseLong(sTime)));
        }
        String context = "";
        context = "???????????????????????????" + user.getName() + "???" + start + "???" + "'" + testCaseWithBLOBs.getName() + "'" + "????????????:" + testCaseComment.getDescription();
        return context;
    }

    public void delete(String commentId) {
        checkCommentOwner(commentId);
        testCaseCommentMapper.deleteByPrimaryKey(commentId);
    }

    public void edit(SaveCommentRequest request) {
        checkCommentOwner(request.getId());
        testCaseCommentMapper.updateByPrimaryKeySelective(request);
    }

    private void checkCommentOwner(String commentId) {
        TestCaseComment comment = testCaseCommentMapper.selectByPrimaryKey(commentId);
        if (!StringUtils.equals(comment.getAuthor(), SessionUtils.getUser().getId())) {
            MSException.throwException(Translator.get("check_owner_comment"));
        }
    }

}
