package cn.edu.neu.shop.pin.service;

import cn.edu.neu.shop.pin.mapper.PinUserMapper;
import cn.edu.neu.shop.pin.model.PinRole;
import cn.edu.neu.shop.pin.model.PinUser;
import cn.edu.neu.shop.pin.util.base.AbstractService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRoleListTransferService extends AbstractService<PinUser> {

    private final PinUserMapper pinUserMapper;

    public UserRoleListTransferService(PinUserMapper pinUserMapper) {
        this.pinUserMapper = pinUserMapper;
    }

    //通过id找用户
    @Override
    public PinUser findById(Integer id) {
        PinUser pinUser = pinUserMapper.findById(id);
        List<PinRole> roles = pinUser.getRoles();
        if (roles != null) {
            pinUser.setRoles(PinUser.transferRoleIntegerToPinRole(roles));
        }
        return pinUser;
    }


    //是否存在有这个Id的用户
    public Boolean existsById(Integer id) {
        return pinUserMapper.existsById(id);
    }

//    public List<PinRole> transfer(JSONArray list) {
//        ArrayList<PinRole> roles = new ArrayList<>();
//        for (int i = 0;i<list.size();i++){
//            roles.add(PinRole.values()[list.getInteger(i)]);
//        }
//        return roles;
//    }
}
