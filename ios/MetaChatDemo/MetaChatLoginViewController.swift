//
//  ViewController.swift
//  MetaChatDemo
//
//  Created by 胡润辰 on 2022/4/21.
//

import UIKit
import AgoraRtcKit
import Zip

let kOnConnectionStateChangedNotifyName = NSNotification.Name(rawValue: "onConnectionStateChanged")

let SCREEN_WIDTH: CGFloat = UIScreen.main.bounds.size.width
let SCREEN_HEIGHT: CGFloat = UIScreen.main.bounds.size.height

class SelSexCell: UIView {
    @IBOutlet weak var selectedBack: UIView!
    @IBOutlet weak var selectedButton: UIButton!
    @IBOutlet weak var selectedLabel: UILabel!
}

protocol SelSexAlertDelegate: NSObjectProtocol {
    func onSelectSex(index: Int)
    
    func onSelectCancel()
}

class SelSexAlert: UIView {
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var selManCell: SelSexCell!
    @IBOutlet weak var selWomanCell: SelSexCell!
    
    public var selIndex: Int = 0
    
    weak var delegate: SelSexAlertDelegate?
        
    @IBAction func selectedAction(sender: UIButton) {
        if sender == selManCell.selectedButton {
            selIndex = 0
            selManCell.selectedBack.isHidden = false
            selWomanCell.selectedBack.isHidden = true
        }else if sender == selWomanCell.selectedButton {
            selIndex = 1
            selManCell.selectedBack.isHidden = true
            selWomanCell.selectedBack.isHidden = false
        }
        
        delegate?.onSelectSex(index: selIndex)
        
        isHidden = true
    }
    
    @IBAction func cancelAction(sender: UIButton) {
        delegate?.onSelectCancel()
        
        isHidden = true
    }
}

class SelAvatarCell: UIView {
    @IBOutlet weak var selectedIcon: UIImageView!
    @IBOutlet weak var selectedButton: UIButton!
}

protocol SelAvatarAlertDelegate: NSObjectProtocol {
    func onSelectAvatar(index: Int)
}

class SelAvatarAlert: UIView {
    @IBOutlet weak var blankButton: UIButton!

    @IBOutlet weak var avatarBoardView: UIView!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var ensureButton: UIButton!
    
    public var selIndex: Int = 0

    weak var delegate: SelAvatarAlertDelegate?
    
    func setUI() {
        avatarBoardView.layer.borderWidth = 1.0
        avatarBoardView.layer.borderColor = UIColor.init(red: 224.0/255.0, green: 216.0/255.0, blue: 203.0/255.0, alpha: 1.0).cgColor
        avatarBoardView.layer.cornerRadius = 4.0
        
        cancelButton.layer.borderWidth = 1.0
        cancelButton.layer.borderColor = UIColor.init(red: 111/255.0, green: 87/255.0, blue: 235/255.0, alpha: 1.0).cgColor
        cancelButton.layer.cornerRadius = 20.0

    }

    
    @IBAction func cancelAction(sender: UIButton) {
        isHidden = true
    }

    @IBAction func selectedAction(sender: UIButton) {
        selIndex = sender.superview?.tag ?? 0;
        
        for subView in avatarBoardView.subviews {
            let avatarCell = subView as! SelAvatarCell
            
            if avatarCell == sender.superview {
                avatarCell.selectedIcon.isHidden = false
            }else {
                avatarCell.selectedIcon.isHidden = true
            }
        }
    }
    
    @IBAction func ensureAction(sender: UIButton) {
        delegate?.onSelectAvatar(index: selIndex)
        
        isHidden = true
    }
}

class MetaChatLoginViewController: UIViewController {
    @IBOutlet weak var selSexAlert: SelSexAlert!
    @IBOutlet weak var selAvatarAlert: SelAvatarAlert!
    
    @IBOutlet weak var selSexLabel: UILabel!
    @IBOutlet weak var selSexIcon: UIImageView!
    @IBOutlet weak var selRoleLabel: UILabel!
    @IBOutlet weak var selRoleIcon: UIImageView!
    
    @IBOutlet weak var avatarImageView: UIImageView!
    @IBOutlet weak var userNameTF: UITextField!
    @IBOutlet weak var errorLabel: UILabel!
    
    @IBOutlet weak var downloadingBack: UIView!
    @IBOutlet weak var downloadingProgress: UIProgressView!
    
    @IBOutlet weak var cancelDownloadButton: UIButton!
    
    #if DEBUG
    private var currentSceneId: Int = 23
    #elseif TEST
    private var currentSceneId: Int = 4
    #else
    private var currentSceneId: Int = 8
    #endif
    
    var sceneVC: MetaChatSceneViewController!
        
    private let libraryPath = NSHomeDirectory() + "/Library/Caches/23"
    
    var selSex: Int = 0    //0未选择，1男，2女
    
    var sceneBroadcastMode = AgoraMetachatSceneBroadcastMode.none // 0: broadcast  1: audience
    
    var currentSelBtnTag: Int = 0
    
    var selAvatarIndex: Int = 0
    
    var avatarUrlArray = ["https://accpic.sd-rtn.com/pic/test/png/2.png", "https://accpic.sd-rtn.com/pic/test/png/4.png", "https://accpic.sd-rtn.com/pic/test/png/1.png", "https://accpic.sd-rtn.com/pic/test/png/3.png", "https://accpic.sd-rtn.com/pic/test/png/6.png", "https://accpic.sd-rtn.com/pic/test/png/5.png"]
    
    var currentSceneInfo: AgoraMetachatSceneInfo?
    var isEntering: Bool = false
    var fromMainScene: Bool = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        userNameTF.attributedPlaceholder = NSAttributedString.init(string: "请输入2-10个字符", attributes: [NSAttributedString.Key.foregroundColor : UIColor.init(red: 161.0/255.0, green: 139.0/255.0, blue: 176/255.0, alpha: 1.0)])
        
        userNameTF.text = "aaaa" + String(Int.random(in: 0...100))
        selSex = 2
        
        selSexAlert.delegate = self
        selAvatarAlert.setUI()
        
        selAvatarAlert.delegate = self
        
        view.addGestureRecognizer(UITapGestureRecognizer.init(target: self, action: #selector(hideKeyboard)))
        
        DispatchQueue.global().async {
//            self.moveFileHandler()
        }
    }

    private func moveFileHandler() {
//        if FileManager.default.fileExists(atPath: libraryPath + "20", isDirectory: nil) {
//            return
//        }
//        FileManager.default.createFile(atPath: libraryPath, contents: nil, attributes: nil)
        let path = Bundle.main.path(forResource: "23", ofType: "zip")
        try? Zip.unzipFile(URL(fileURLWithPath: path ?? ""), destination: URL(fileURLWithPath: NSHomeDirectory() + "/Library/Caches/"), overwrite: true, password: nil) { progress in
            print("zip progress = \(progress)")
        } fileOutputHandler: { unzippedFile in
            print(unzippedFile.path)
        }
    }
    
    override var shouldAutorotate: Bool {
        return false
    }
    
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    override var prefersStatusBarHidden: Bool {
        false
    }

    @objc func hideKeyboard() {
        view.endEditing(true)
    }
    
    @IBAction func selectedSexAction(sender: UIButton) {
        var frame = selSexAlert.frame
        if sender.tag == 1001 {
            selSexIcon.image = UIImage.init(named: "arrow-up")
            frame.origin.y = 0
            selSexAlert.selManCell.selectedLabel.text = "男"
            selSexAlert.selWomanCell.selectedLabel.text = "女"
        } else if sender.tag == 1002 {
            selRoleIcon.image = UIImage.init(named: "arrow-up")
            frame.origin.y = 80
            selSexAlert.selManCell.selectedLabel.text = "主播"
            selSexAlert.selWomanCell.selectedLabel.text = "观众"
        }
//        selSexAlert.frame = frame
        currentSelBtnTag = sender.tag
        view.endEditing(true)
        
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.01, execute: {
            self.selSexAlert.isHidden = false
            self.selSexAlert.frame = frame
        })
    }
    
    @IBAction func selectedAvatarAction(sender: UIButton) {
        selAvatarAlert.isHidden = false
    }
    
    @IBAction func cancelDownloadHandler(_ sender: Any) {
        downloadingBack.isHidden = true
        MetaChatEngine.sharedEngine.metachatKit?.cancelDownloadScene(currentSceneId)
    }
    
    func checkValid() -> Bool {
        let nameCount = userNameTF.text?.count ?? 0
        if (nameCount < 2) || (nameCount > 10) {
            errorLabel.text = "姓名必须包含2-10个字符"
            return false
        }
        
        if selSex == 0 {
            errorLabel.text = "请选择性别"
            return false
        }
        
        errorLabel.text = nil
        return true
    }
        
    var indicatorView: UIActivityIndicatorView?
    
    @IBAction func enterScene(sender: UIButton) {
        if !checkValid() {
            return
        }
        
        if isEntering {
            return
        }
        isEntering = true
        
        indicatorView = UIActivityIndicatorView.init(frame: view.frame)
        if #available(iOS 13.0, *) {
            indicatorView?.style = UIActivityIndicatorView.Style.large
        } else {
            // Fallback on earlier versions
        }
        indicatorView?.color = UIColor.white
        view.addSubview(indicatorView!)
        indicatorView?.startAnimating()
        
        let defaultStand = UserDefaults.standard
        let key = "isAppEnterScene"
        let isEnter = defaultStand.bool(forKey: key)
        if !isEnter {
            defaultStand.set(true, forKey: key)
        }
        
//        MetaChatEngine.sharedEngine.resolution = CGSizeMake(view.bounds.size.width * UIScreen.main.scale, view.bounds.size.height * UIScreen.main.scale)
        MetaChatEngine.sharedEngine.resolution = CGSizeMake(240, 240);
        
        MetaChatEngine.sharedEngine.createRtcEngine()
        
        MetaChatEngine.sharedEngine.createMetachatKit(userName: userNameTF.text!, avatarUrl: avatarUrlArray[selAvatarIndex], delegate: self)
        
//        if sceneBroadcastMode == .audience {
//            let mockRenderView = MockRenderView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height))
//            mockRenderView.layer.contentsScale = UIScreen.main.scale
//            MetaChatEngine.sharedEngine.mockRenderView = mockRenderView
//            onSceneReady(AgoraMetachatSceneInfo())
//            
//            indicatorView?.stopAnimating()
//            isEntering = false
//            return
//        }
                        
        MetaChatEngine.sharedEngine.metachatKit?.getSceneInfos()
        
        if self.hasUserDressInfo(self.selSex) && self.fromMainScene == false {
            kSceneIndex = 0
            switchOrientation(isPortrait: false, isFullScreen: isEnter)
        } else {
            kSceneIndex = 1
            switchOrientation(isPortrait: true, isFullScreen: isEnter)
        }
    }
    
    func onSceneReady(_ sceneInfo: AgoraMetachatSceneInfo) {
        self.joinChannel(sceneInfo)
    }
    
    func joinChannel(_ sceneInfo: AgoraMetachatSceneInfo) {
        MetaChatEngine.sharedEngine.joinRtcChannel { [weak self] in
            guard let wSelf = self else {return}
            wSelf.createScene(sceneInfo)
        }
    }
    
    func createScene(_ sceneInfo: AgoraMetachatSceneInfo) {
        DispatchQueue.main.async {
            self.downloadingBack.isHidden = true
            
            let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
            guard let sceneViewController = storyBoard.instantiateViewController(withIdentifier: "SceneViewController") as? MetaChatSceneViewController else { return }
            sceneViewController.currentGender = self.selSex - 1
            sceneViewController.sceneBroadcastMode = self.sceneBroadcastMode
            sceneViewController.delegate = self
            sceneViewController.modalPresentationStyle = .fullScreen
            MetaChatEngine.sharedEngine.createScene(sceneViewController, sceneBroadcastMode: self.sceneBroadcastMode)
            MetaChatEngine.sharedEngine.currentSceneInfo = sceneInfo
            self.sceneVC = sceneViewController
        }
    }
    
    /// 本地是否存在换装信息
    func hasUserDressInfo(_ selSex: Int) -> Bool {
        let defaultStand = UserDefaults.standard
        let key = (selSex - 1 == 1) ? "mc_userDressInfo_girl" : "mc_userDressInfo_boy"
        let info = defaultStand.getObject(forKey: key) as [UserDressInfo]
        if info.count > 0 && info[0].gender == (selSex - 1) {
            return true
        }
        return false
    }
}

extension MetaChatLoginViewController: SelSexAlertDelegate {
    func onSelectCancel() {
        selSexIcon.image = UIImage.init(named: "arrow-down")
        selRoleIcon.image = UIImage.init(named: "arrow-down")
    }
    
    func onSelectSex(index: Int) {
        selSex = index + 1
        sceneBroadcastMode = AgoraMetachatSceneBroadcastMode.init(rawValue: UInt(index)) ?? .none
        
        if currentSelBtnTag == 1001 {
            if selSex == 1 {
                selSexLabel.text = "男"
            } else if selSex == 2 {
                selSexLabel.text = "女"
            }
        } else {
            if selSex == 1 {
                selRoleLabel.text = "主播"
            } else if selSex == 2 {
                selRoleLabel.text = "观众"
            }
        }
        
        selSexIcon.image = UIImage.init(named: "arrow-down")
        selRoleIcon.image = UIImage.init(named: "arrow-down")
    }
}


extension MetaChatLoginViewController: SelAvatarAlertDelegate {
    func onSelectAvatar(index: Int) {
        selAvatarIndex = index
        
        let localImageName = "avatar\(index+1)"
        avatarImageView.image = UIImage.init(named: localImageName)
    }
}

extension MetaChatLoginViewController: AgoraMetachatEventDelegate {
    func onCreateSceneResult(_ scene: AgoraMetachatScene?, errorCode: Int) {
        if errorCode != 0 {
            print("create scene error: \(errorCode)")
            return
        }
        
        MetaChatEngine.sharedEngine.metachatScene = scene
        DispatchQueue.main.async {
            if self.presentedViewController == nil {
                self.present(self.sceneVC, animated: true)
            }
        }
    }
    
    func onConnectionStateChanged(_ state: AgoraMetachatConnectionStateType, reason: AgoraMetachatConnectionChangedReasonType) {
        if state == .disconnected {
            DispatchQueue.main.async {
                self.indicatorView?.stopAnimating()
                self.indicatorView?.removeFromSuperview()
                self.indicatorView = nil
            }
        } else if state == .aborted {
            MetaChatEngine.sharedEngine.leaveRtcChannel()
            MetaChatEngine.sharedEngine.leaveScene()
            DispatchQueue.main.async {
                DLog("state == \(state.rawValue), reason == \(reason.rawValue)")
                NotificationCenter.default.post(name: kOnConnectionStateChangedNotifyName, object: nil, userInfo: ["state":state.rawValue,"reason":reason.rawValue])
                self.dismiss(animated: true, completion: nil)
            }
        }
    }
    
    func onRequestToken() {
        
    }
    
    func onGetSceneInfosResult(_ scenes: NSMutableArray, errorCode: Int) {
        self.isEntering = false
        
        DispatchQueue.main.async {
            self.indicatorView?.stopAnimating()
            self.indicatorView?.removeFromSuperview()
            self.indicatorView = nil
        }
        
        if errorCode != 0 {
            DispatchQueue.main.async {
                let alertController = UIAlertController.init(title: "get Scenes failed:errorcode:\(errorCode)", message:nil , preferredStyle:.alert)
                
                alertController.addAction(UIAlertAction.init(title: "确定", style: .cancel, handler: nil))
                let appDelegate = UIApplication.shared.delegate as! AppDelegate
                appDelegate.isFullScreen = false
                self.present(alertController, animated: true)
            }
            return
        }
        
        if scenes.count == 0 {
            return
        }
        
        guard let firstScene = scenes.compactMap({ $0 as? AgoraMetachatSceneInfo }).first(where: { $0.sceneId == currentSceneId }) else {
            return
        }
        
        currentSceneInfo = firstScene
        
        let metachatKit = MetaChatEngine.sharedEngine.metachatKit
        let totalSize = firstScene.totalSize / 1024 / 1024
        if metachatKit?.isSceneDownloaded(currentSceneId) != 1 {
            DispatchQueue.main.async {
                let alertController = UIAlertController.init(title: "下载提示", message: "首次进入MetaChat场景需下载\(totalSize)M数据包", preferredStyle:.alert)
                
                alertController.addAction(UIAlertAction.init(title: "下次再说", style: .cancel, handler: nil))
                alertController.addAction(UIAlertAction.init(title: "立即下载", style: .default, handler: { UIAlertAction in
                    metachatKit?.downloadScene(self.currentSceneId)
                    self.downloadingBack.isHidden = false
                }))
                
                let appDelegate = UIApplication.shared.delegate as! AppDelegate
                appDelegate.isFullScreen = false
                
                self.present(alertController, animated: true)
            }
        }else {
            onSceneReady(firstScene)
        }
    }
    
    func onDownloadSceneProgress(_ sceneId: Int, progress: Int, state: AgoraMetachatDownloadStateType) {
        DispatchQueue.main.async {
            self.downloadingProgress.progress = Float(progress)/100.0
        }
        
        if state == .downloaded && currentSceneInfo != nil {
            onSceneReady(currentSceneInfo!)
        }
    }
}

/// 保存换装信息，并重新进入主场景
extension MetaChatLoginViewController: handleDressInfoDelegate {
    func storeDressInfo(_ fromMainScene: Bool) {
        self.fromMainScene = fromMainScene
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.enterScene(sender: UIButton())
        }
    }
}
