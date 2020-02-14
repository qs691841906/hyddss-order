package com.sinosoft.ddss.service;

import javax.jws.WebService;

@WebService(name = "orderWebService", // 暴露服务名称
		targetNamespace = "http://service.ddss.sinosoft.com/"// 命名空间,一般是接口的包名倒序
)
public interface OrderWebService {
	/**
	 * 与共享：订单提交
	 * 
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_ORDERSUBMIT(String xmlInfo);

	/**
	 * 与共享：订单集信息检索与反馈
	 * 
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_ORDERSETRETRIEVAL(String xmlInfo);

	/**
	 * 与共享：加入购物车
	 * 
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_ADDSHOPCAR(String xmlInfo);

	/**
	 * 与共享：购物车查询
	 * 
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_SHOPCARRIEVAL(String xmlInfo);

	/**
	 * 与共享：购物车删除
	 * 
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_SHOPCARDELETE(String xmlInfo);

	/**
	 * 与共享：快速观测需求
	 * @param xmlInfo
	 * @return
	 */
	String DDSS_SSP_OBSTASK(String xmlInfo);
	
	
	/**
	 * 手机端总接口，负责根据token拦截和分配访问
	 * @param xmlInfo
	 * @return
	 */
	String SSP_DDSS_MOBILE(String xmlInfo);
}
