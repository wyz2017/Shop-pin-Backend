package cn.edu.neu.shop.pin.controller.admin;

import cn.edu.neu.shop.pin.model.PinStore;
import cn.edu.neu.shop.pin.model.PinUser;
import cn.edu.neu.shop.pin.service.StoreCloseBatchService;
import cn.edu.neu.shop.pin.service.StoreService;
import cn.edu.neu.shop.pin.service.security.UserService;
import cn.edu.neu.shop.pin.util.PinConstants;
import cn.edu.neu.shop.pin.util.ResponseWrapper;
import cn.edu.neu.shop.pin.util.img.ImgUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

@RestController
@RequestMapping(value = "/manager/store")
public class AdminStoreController {

    private final UserService userService;

    private final StoreService storeService;

    private final StoreCloseBatchService storeCloseBatchService;

    @Autowired
    public AdminStoreController(UserService userService, StoreService storeService, StoreCloseBatchService storeCloseBatchService) {
        this.userService = userService;
        this.storeService = storeService;
        this.storeCloseBatchService = storeCloseBatchService;
    }

    /**
     * 得到这个商人所有的商铺
     *
     * @param req 传入的request
     * @return 返回所有的商铺
     */
    @GetMapping("/storeList")
    public JSONObject getProducts(HttpServletRequest req) {
        try {
            PinUser user = userService.whoAmI(req);
            JSONObject data = new JSONObject();
            data.put("storeList", storeService.getStoreListByOwnerId(user.getId()));
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    /**
     * 新增店铺
     */
    @PostMapping("/storeInfo")
    public JSONObject addStoreInfo(HttpServletRequest httpServletRequest, @RequestBody JSONObject requestJSON) {
        try {
            PinUser user = userService.whoAmI(httpServletRequest);
            String storeName = requestJSON.getString("name");
            String description = requestJSON.getString("description");
            String phone = requestJSON.getString("phone");
            String email = requestJSON.getString("email");
            String base64Img = requestJSON.getString("image");
            String url = requestJSON.getString("url");
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS,
                    storeService.addStoreInfo(storeName, description, phone, email, url, user.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    /**
     * 修改店铺信息
     *
     * @param requestJSON 请求体JSON
     * @return 响应JSON
     */
    @PutMapping("/storeInfo")
    public JSONObject updateStoreInfo(@RequestBody JSONObject requestJSON) {
        try {
            PinStore store = JSONObject.toJavaObject(requestJSON, PinStore.class);
            if (storeService.update(store) == null) {
                return ResponseWrapper.wrap(PinConstants.StatusCode.PERMISSION_DENIED, PinConstants.ResponseMessage.PERMISSION_DENIED, null);
            }
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @GetMapping("/close-batch")
    public JSONObject getGruopCloseBatchTime(HttpServletRequest httpServletRequest) {
        try {
            String store = httpServletRequest.getHeader("Current-Store");
            Integer storeId = Integer.parseInt(store);
            JSONObject data = new JSONObject();
            data.put("list", storeCloseBatchService.getGroupCloseBatchTime(storeId));
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, data);
        } catch (Exception e) {
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @PostMapping("/upload")
    public JSONObject uploadStoreImage(@RequestBody JSONObject uploadingInfo) {
        //截掉 "data:image/png;base64,"
        String base64Img = uploadingInfo.getString("image");
        JSONObject data = new JSONObject();
        data.put("url",ImgUtil.upload(base64Img));
        return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, data);
    }

    @DeleteMapping("/close-batch")
    public JSONObject deleteGroupCloseBatchTime(HttpServletRequest httpServletRequest, @RequestBody JSONObject requestJSON) {
        try {
            Integer storeId = Integer.valueOf(httpServletRequest.getHeader("Current-Store"));
            JSONArray array = requestJSON.getJSONArray("closeBatchList");
            for (int i = 0; i < array.size(); i++) {
                Integer id = array.getJSONObject(i).getInteger("id");
                storeCloseBatchService.deleteGroupCloseBatch(storeId, id);
            }
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, null);
        } catch (Exception e) {
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @PostMapping("/close-batch")
    public JSONObject addGroupCloseBatchTime(HttpServletRequest httpServletRequest, @RequestBody JSONObject requestJSON) {
        try {
            Integer storeId = Integer.valueOf(httpServletRequest.getHeader("Current-Store"));
            Date date = requestJSON.getDate("closeBatch");
            storeCloseBatchService.addGroupCloseBatch(storeId, date);
//            JSONObject data = new JSONObject();
//            data.put("closeBatch", data);
            return ResponseWrapper.wrap(PinConstants.StatusCode.SUCCESS, PinConstants.ResponseMessage.SUCCESS, null);
        } catch (Exception e) {
            return ResponseWrapper.wrap(PinConstants.StatusCode.INTERNAL_ERROR, e.getMessage(), null);
        }
    }
}
