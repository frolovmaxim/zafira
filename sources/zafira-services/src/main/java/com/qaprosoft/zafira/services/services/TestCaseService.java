package com.qaprosoft.zafira.services.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qaprosoft.zafira.dbaccess.dao.mysql.TestCaseMapper;
import com.qaprosoft.zafira.dbaccess.model.TestCase;
import com.qaprosoft.zafira.dbaccess.model.User;
import com.qaprosoft.zafira.services.exceptions.ServiceException;

@Service
public class TestCaseService
{
	@Autowired
	private TestCaseMapper testCaseMapper;
	
	@Autowired
	private UserService userService;
	
	@Transactional(rollbackFor = Exception.class)
	public void createTestCase(TestCase testCase) throws ServiceException
	{
		testCaseMapper.createTestCase(testCase);
	}
	
	@Transactional(readOnly = true)
	public TestCase getTestCaseById(long id) throws ServiceException
	{
		return testCaseMapper.getTestCaseById(id);
	}
	
	@Transactional(readOnly = true)
	public TestCase getTestCaseByClassAndMethod(String testClass, String testMethod) throws ServiceException
	{
		return testCaseMapper.getTestCaseByClassAndMethod(testClass, testMethod);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public TestCase updateTestCase(TestCase testCase) throws ServiceException
	{
		testCaseMapper.updateTestCase(testCase);
		return testCase;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteTestCase(TestCase testCase) throws ServiceException
	{
		testCaseMapper.deleteTestCase(testCase);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public TestCase [] initiateTestCases(TestCase [] newTestCases) throws ServiceException
	{
		int index = 0;
		for(TestCase newTestCase : newTestCases)
		{
			User user = userService.createUser(newTestCase.getUser().getUserName());
			newTestCase.setUser(user);
			TestCase testCase = getTestCaseByClassAndMethod(newTestCase.getTestClass(), newTestCase.getTestMethod());
			if(testCase == null)
			{
				createTestCase(newTestCase);
			}
			else if(!testCase.equals(newTestCase))
			{
				newTestCase.setId(testCase.getId());
				updateTestCase(newTestCase);
			}
			else
			{
				newTestCase = testCase;
			}
			newTestCases[index++] = newTestCase;
		}
		return newTestCases;
	}
}
