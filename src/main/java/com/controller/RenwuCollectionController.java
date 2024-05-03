
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 任务收藏
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/renwuCollection")
public class RenwuCollectionController {
    private static final Logger logger = LoggerFactory.getLogger(RenwuCollectionController.class);

    private static final String TABLE_NAME = "renwuCollection";

    @Autowired
    private RenwuCollectionService renwuCollectionService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GonggaoService gonggaoService;//任务资讯公告
    @Autowired
    private JiequyonghuService jiequyonghuService;//接取用户
    @Autowired
    private RenwuService renwuService;//任务
    @Autowired
    private RenwuChatService renwuChatService;//任务咨询
    @Autowired
    private RenwuCommentbackService renwuCommentbackService;//任务评价
    @Autowired
    private RenwuOrderService renwuOrderService;//任务订单
    @Autowired
    private FabuyonghuService fabuyonghuService;//发布用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("接取用户".equals(role))
            params.put("jiequyonghuId",request.getSession().getAttribute("userId"));
        else if("发布用户".equals(role))
            params.put("fabuyonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = renwuCollectionService.queryPage(params);

        //字典表数据转换
        List<RenwuCollectionView> list =(List<RenwuCollectionView>)page.getList();
        for(RenwuCollectionView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        RenwuCollectionEntity renwuCollection = renwuCollectionService.selectById(id);
        if(renwuCollection !=null){
            //entity转view
            RenwuCollectionView view = new RenwuCollectionView();
            BeanUtils.copyProperties( renwuCollection , view );//把实体数据重构到view中
            //级联表 任务
            //级联表
            RenwuEntity renwu = renwuService.selectById(renwuCollection.getRenwuId());
            if(renwu != null){
            BeanUtils.copyProperties( renwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "jiequyonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setRenwuId(renwu.getId());
            }
            //级联表 接取用户
            //级联表
            JiequyonghuEntity jiequyonghu = jiequyonghuService.selectById(renwuCollection.getJiequyonghuId());
            if(jiequyonghu != null){
            BeanUtils.copyProperties( jiequyonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "jiequyonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setJiequyonghuId(jiequyonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody RenwuCollectionEntity renwuCollection, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,renwuCollection:{}",this.getClass().getName(),renwuCollection.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("接取用户".equals(role))
            renwuCollection.setJiequyonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<RenwuCollectionEntity> queryWrapper = new EntityWrapper<RenwuCollectionEntity>()
            .eq("renwu_id", renwuCollection.getRenwuId())
            .eq("jiequyonghu_id", renwuCollection.getJiequyonghuId())
            .eq("renwu_collection_types", renwuCollection.getRenwuCollectionTypes())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        RenwuCollectionEntity renwuCollectionEntity = renwuCollectionService.selectOne(queryWrapper);
        if(renwuCollectionEntity==null){
            renwuCollection.setInsertTime(new Date());
            renwuCollection.setCreateTime(new Date());
            renwuCollectionService.insert(renwuCollection);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody RenwuCollectionEntity renwuCollection, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,renwuCollection:{}",this.getClass().getName(),renwuCollection.toString());
        RenwuCollectionEntity oldRenwuCollectionEntity = renwuCollectionService.selectById(renwuCollection.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("接取用户".equals(role))
//            renwuCollection.setJiequyonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

            renwuCollectionService.updateById(renwuCollection);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<RenwuCollectionEntity> oldRenwuCollectionList =renwuCollectionService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        renwuCollectionService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer jiequyonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<RenwuCollectionEntity> renwuCollectionList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            RenwuCollectionEntity renwuCollectionEntity = new RenwuCollectionEntity();
//                            renwuCollectionEntity.setRenwuId(Integer.valueOf(data.get(0)));   //任务 要改的
//                            renwuCollectionEntity.setJiequyonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            renwuCollectionEntity.setRenwuCollectionTypes(Integer.valueOf(data.get(0)));   //类型 要改的
//                            renwuCollectionEntity.setInsertTime(date);//时间
//                            renwuCollectionEntity.setCreateTime(date);//时间
                            renwuCollectionList.add(renwuCollectionEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        renwuCollectionService.insertBatch(renwuCollectionList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = renwuCollectionService.queryPage(params);

        //字典表数据转换
        List<RenwuCollectionView> list =(List<RenwuCollectionView>)page.getList();
        for(RenwuCollectionView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        RenwuCollectionEntity renwuCollection = renwuCollectionService.selectById(id);
            if(renwuCollection !=null){


                //entity转view
                RenwuCollectionView view = new RenwuCollectionView();
                BeanUtils.copyProperties( renwuCollection , view );//把实体数据重构到view中

                //级联表
                    RenwuEntity renwu = renwuService.selectById(renwuCollection.getRenwuId());
                if(renwu != null){
                    BeanUtils.copyProperties( renwu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setRenwuId(renwu.getId());
                }
                //级联表
                    JiequyonghuEntity jiequyonghu = jiequyonghuService.selectById(renwuCollection.getJiequyonghuId());
                if(jiequyonghu != null){
                    BeanUtils.copyProperties( jiequyonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setJiequyonghuId(jiequyonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody RenwuCollectionEntity renwuCollection, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,renwuCollection:{}",this.getClass().getName(),renwuCollection.toString());
        Wrapper<RenwuCollectionEntity> queryWrapper = new EntityWrapper<RenwuCollectionEntity>()
            .eq("renwu_id", renwuCollection.getRenwuId())
            .eq("jiequyonghu_id", renwuCollection.getJiequyonghuId())
            .eq("renwu_collection_types", renwuCollection.getRenwuCollectionTypes())
//            .notIn("renwu_collection_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        RenwuCollectionEntity renwuCollectionEntity = renwuCollectionService.selectOne(queryWrapper);
        if(renwuCollectionEntity==null){
            renwuCollection.setInsertTime(new Date());
            renwuCollection.setCreateTime(new Date());
        renwuCollectionService.insert(renwuCollection);

            return R.ok();
        }else {
            return R.error(511,"您已经收藏过了");
        }
    }

}
