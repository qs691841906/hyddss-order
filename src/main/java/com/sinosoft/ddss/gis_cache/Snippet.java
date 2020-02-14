//package com.sinosoft.ddss.gis_cache;
//
//import java.io.File;
//import java.util.List;
//
//import javax.annotation.PostConstruct;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.sinosoft.ddss.common.entity.Metadata;
//import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
//import com.sinosoft.ddss.service.impl.OrderServiceImpl;
//import com.sinosoft.ddss.utils.FileUtil;
//
//@Service("Snippet")
//public class Snippet {
//	@Autowired
//	private ddssMetadataMapper ddssmetadatamapper;
//	private static Logger log = LoggerFactory.getLogger(Snippet.class);
//	@PostConstruct
//	public void gis() {
//
//		int i = 0;
//		while (true) {
//			log.info(String.valueOf(i));
////			依次递增查询元数据
//			List<Metadata> list_metadata = ddssmetadatamapper.selectAllMetadata(i);
////			如果查询到的元数据是空的说明数据已经查询完毕
//			if (list_metadata == null || list_metadata.size() < 1) {
//				break;
//			}
//
//			for (Metadata metadata : list_metadata) {
//				try {
////					数据在GIS_CACHE的根目录下
//					String url = File.separator + "GIS_CACHE" + File.separator;
////					将开始采集时间作为存放数据的目录
//					String imageStartTime = metadata.getImageStartTime().split(" ")[0].replace("/", "").replace("-",
//							"");
//					
//					
////					获取质检报告路径
//					String rep = metadata.getRepDownloadAdd();
//					if(rep.contains(imageStartTime+"/")){
//						log.info(rep+"包含"+imageStartTime+"/");
//						continue;
//					}
//					boolean rep_flag = rep != null && !rep.equals("");
//					if (rep_flag) {
////						截取到最后一个文件名称
//						String[] reps = rep.split("/");
//						rep = reps[reps.length - 1];
//						File file_rep = new File(url + rep);
////						如果文件存在就将文件拷贝到ftp对应的目录下
//						if (file_rep.isFile()) {
//							log.info(file_rep+"存在");
//							String rep_url_s = "/DDS_CACHE/repDownloadAdd/" + imageStartTime;
//							File rep_url_s_f = new File(rep_url_s);
//							if(!rep_url_s_f.exists()){
//								log.info(rep_url_s_f+"不存在，创建");
//								rep_url_s_f.mkdirs();
//							}
//							String file_rep_url = rep_url_s + File.separator + file_rep.getName();
//							FileUtil.copyFile(file_rep.toString(), file_rep_url);
//							log.info(file_rep+"拷贝到"+file_rep_url+"成功");
//							String ftp_url = "ftp://repDownloadAdd:ea09792a17e025712fb70d25a881431b@111.202.150.82:2121/"+imageStartTime+File.separator;
//							metadata.setRepDownloadAdd(ftp_url+rep);
//							file_rep.delete();
//							log.info(file_rep+"删除");
//						}else{
//							log.info(file_rep+"不存在");
//							continue;
//						}
//					}
//					
//					
//					
////					获取拇指图路径
//					String thumb = metadata.getThumbFileUrl();
//					if(thumb.contains(imageStartTime+"/")){
//						log.info(thumb+"包含"+imageStartTime+"/");
//						continue;
//					}
//					boolean thumb_flag = thumb != null && !thumb.equals("");
//					if (thumb_flag) {
//						File file_thumb = new File(url + thumb);
//						String file_thumb_url = url + imageStartTime + File.separator + file_thumb.getName();
////						如果目录下有该文件就将其拷贝到时间目录下，并删除旧的文件
//						if (file_thumb.isFile()) {
//							log.info(file_thumb+"存在");
//							
//							File time_file = new File(url+imageStartTime);
//							if(!time_file.exists()){
//								log.info(time_file+"不存在，创建");
//								time_file.mkdirs();
//							}
//							
//							FileUtil.copyFile(file_thumb.toString(), file_thumb_url);
//							log.info(file_thumb+"拷贝到"+file_thumb_url+"成功");
//							file_thumb.delete();
//							log.info(file_thumb+"删除");
//							metadata.setThumbFileUrl(imageStartTime + File.separator + file_thumb.getName());
//						}
//					}
////					获取快试图路径
//					String quick = metadata.getQuickFileUrl();
//					if(quick.contains(imageStartTime+"/")){
//						log.info(quick+"包含"+imageStartTime+"/");
//						continue;
//					}
//					boolean quick_flag = quick != null && !quick.equals("");
//					if (quick_flag) {
//						File file_quick = new File(url + quick);
//						String file_quick_url = url + imageStartTime + File.separator + file_quick.getName();
////						如果目录下有该文件就将其拷贝到时间目录下，并删除旧的文件
//						if (file_quick.isFile()) {
//							log.info(file_quick+"存在");
//							
//							File time_file = new File(url+imageStartTime);
//							if(!time_file.exists()){
//								log.info(time_file+"不存在，创建");
//								time_file.mkdirs();
//							}
//							
//							FileUtil.copyFile(file_quick.toString(), file_quick_url);
//							log.info(file_quick+"拷贝到"+file_quick_url+"成功");
//							file_quick.delete();
//							log.info(file_quick+"删除");
//							metadata.setQuickFileUrl(imageStartTime + File.separator + file_quick.getName());
//						}else{
//							log.info(file_quick+"不存在");
//						}
//					}
//
//
//					// 修改数据的拇指图和快试图
//					if (thumb_flag || quick_flag || rep_flag) {
//						ddssmetadatamapper.updateMetadataFileUrl(metadata);
//						log.info("修改数据成功"+metadata);
//					}
//				} catch (Exception e) {
//					log.error(e.getMessage());
//					
//				}
//			}
//			i = i + 1000;
//		}
//	}
//}
