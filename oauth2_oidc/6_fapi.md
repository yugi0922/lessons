# 6. FAPI (Financial-grade API)

## 1. FAPIとは

FAPI (Financial-grade API) は、金融サービス向けに設計された高度なセキュリティプロファイルです。OAuth 2.0とOpenID Connectの上に構築され、金融取引に必要な厳格なセキュリティ要件を満たすための技術仕様を定義しています。

### 主な特徴
- **高度なセキュリティ**: 金融取引レベルのセキュリティ保証
- **標準準拠**: OAuth 2.0/OpenID Connectとの完全な互換性
- **実装の明確性**: 曖昧さを排除した具体的な実装要件
- **相互運用性**: グローバルな金融エコシステムでの相互運用

### FAPIの位置づけ
```
アプリケーションレイヤー
    ↓
[ FAPI Security Profile ]
    ↓
[ OpenID Connect ]
    ↓
[ OAuth 2.0 ]
    ↓
トランスポートレイヤー (TLS)
```

## 2. 構成要素

FAPIは3つの主要な構成要素から成り立っています：

### 2.1 Security Profile（セキュリティプロファイル）
OAuth 2.0とOpenID Connectの具体的な実装要件を定義する技術仕様。必須機能、推奨機能、オプション機能を明確に区別し、実装の曖昧さを排除します。

### 2.2 Attacker Model（攻撃者モデル）
想定される攻撃者の能力と攻撃シナリオを定義。各攻撃に対する具体的な防御策を示し、セキュリティ設計の根拠を提供します。

### 2.3 Implementation Advice（実装アドバイス）
実装者向けのベストプラクティスとガイダンス。一般的な実装ミスの回避方法や、パフォーマンスとセキュリティのバランスに関する指針を提供します。

## 3. セキュリティプロファイル

### 3.1 FAPI 1.0 プロファイル

FAPI 1.0は、用途に応じて2つのプロファイルを定義しています：

#### プロファイル比較マトリックス

| 機能カテゴリ | 要件 | Baseline (Read-Only) | Advanced (Read-Write) |
|------------|------|---------------------|---------------------|
| **基本要件** |
| TLSバージョン | | TLS 1.2以上 (必須) | TLS 1.2以上 (必須) |
| クライアント認証 | | 方式は実装依存 | MTLS or Private Key JWT (必須) |
| **PKCE** |
| PKCEサポート | | S256 (必須) | S256 (必須) |
| Plainメソッド | | 禁止 | 禁止 |
| **認可要求** |
| State パラメータ | | 必須 | 必須 |
| Nonce パラメータ | | 必須 (OIDC) | 必須 (OIDC) |
| リダイレクトURI | | 完全一致 (必須) | 完全一致 (必須) |
| JAR (JWT Request) | | オプション | 必須 |
| Request URI | | オプション | 推奨 |
| **認可応答** |
| JARM (JWT Response) | | 非対応 | 必須 |
| Response Mode | | query/fragment | JWT |
| **トークン** |
| 送信者制約 | | 非対応 | MTLS (必須) |
| トークン有効期限 | | 実装依存 | 短期間推奨 |
| Refresh Token | | オプション | 送信者制約付き |
| **その他** |
| PAR | | オプション | 推奨 |
| CIBA | | 非対応 | オプション |
| 用途 | | 参照系API | 更新系API |

### 3.2 FAPI 2.0 プロファイル

FAPI 2.0は、プロファイルを統一し、より柔軟な実装オプションを提供：

#### FAPI 2.0 要件マトリックス

| 機能カテゴリ | 要件 | ステータス | 備考 |
|------------|------|----------|-----|
| **必須要件** |
| PAR | Pushed Authorization Request | 必須 | 全ての認可要求で使用 |
| PKCE | S256 | 必須 | Plainは禁止 |
| 送信者制約 | MTLS or DPoP | 必須 | いずれか一方を選択 |
| JAR | JWT Secured Request | 必須 | 署名必須、暗号化は推奨 |
| **推奨要件** |
| JARM | JWT Secured Response | 推奨 | 高セキュリティ環境で推奨 |
| RAR | Rich Authorization Request | 推奨 | 詳細な権限管理 |
| Grant Management | | 推奨 | 認可の管理API |
| **オプション要件** |
| CIBA | Client Initiated Backchannel | オプション | デカップルドフロー |
| Lodging Intent | | オプション | 事前意図登録 |

### 3.3 セキュリティプロファイルの選択フロー

```
スタート
    ↓
APIの用途は？
    ├─ 読み取り専用
    │   ├─ 個人情報含む → FAPI 1.0 Baseline
    │   └─ 公開情報のみ → OAuth 2.0で十分
    └─ 読み書き可能
        ├─ 金融取引あり → FAPI 1.0 Advanced or FAPI 2.0
        └─ 設定変更のみ → FAPI 1.0 Baseline検討
```

## 4. 攻撃者モデル

### 4.1 攻撃者の分類と能力

#### A1: ネットワーク攻撃者
**能力**:
- ネットワークトラフィックの盗聴・記録
- DNSの改ざん、なりすまし
- 中間者攻撃（MITM）の実行
- パケットの遅延・順序変更

**攻撃シナリオ**:
```
[ユーザー] ---(1)認証情報---> [攻撃者] ---(2)改ざん---> [サーバー]
              ↑                    |
              +---(3)偽レスポンス---+
```

**FAPI対策**:
- TLS 1.2以上の必須化
- 証明書ピンニング
- HSTS (HTTP Strict Transport Security)

#### A2: Web攻撃者
**能力**:
- 悪意のあるWebサイトの運営
- ユーザーのブラウザ上でのJavaScript実行
- フィッシングサイトの構築
- ソーシャルエンジニアリング

**攻撃シナリオ**:
```
正規サイト: auth.bank.com
攻撃サイト: auth-bank.com (類似ドメイン)
    ↓
[ユーザー] → [攻撃サイト] → 認証情報窃取
    ↓
[攻撃者] → [正規サイト] → 不正アクセス
```

**FAPI対策**:
- State/Nonceによるセッション固定攻撃対策
- PKCEによる認可コード横取り対策
- 厳密なリダイレクトURI検証

#### A3: アプリケーション攻撃者
**能力**:
- 悪意のあるOAuth/OIDCクライアントの作成
- 正規クライアントになりすまし
- 脆弱なクライアントの悪用
- クライアント認証情報の窃取

**攻撃パターン**:
```
1. 動的クライアント登録の悪用
   攻撃者 → 登録API → 悪意のあるリダイレクトURI登録

2. クライアント認証情報の漏洩
   Public Client → Secret漏洩 → なりすまし

3. 認可範囲の拡大
   最小権限 → 権限昇格攻撃 → 過剰な権限取得
```

**FAPI対策**:
- 動的登録の無効化または制限
- Confidential Clientの必須化
- クライアント認証の強化（MTLS、Private Key JWT）

### 4.2 具体的な攻撃シナリオと対策

#### シナリオ1: 認可コード横取り攻撃

```
攻撃フロー:
1. [ユーザー] → [正規Client] → 認可リクエスト
2. [認可サーバー] → リダイレクト → [攻撃者のサイト]
3. [攻撃者] → 認可コード取得 → [トークンエンドポイント]
4. [攻撃者] → アクセストークン取得 → 不正アクセス

FAPI対策:
- PKCE (S256) 必須
- リダイレクトURIの完全一致検証
- 認可コードの一回限り使用
- 短い有効期限（60秒以内）
```

#### シナリオ2: トークンリプレイ攻撃

```
攻撃フロー:
1. [正規ユーザー] → Bearer Token → [API]
2. [攻撃者] → トークン盗聴
3. [攻撃者] → 盗んだToken → [API] → 不正アクセス

FAPI対策:
- 送信者制約（Sender Constrained Token）
  - MTLS: クライアント証明書とトークンの紐付け
  - DPoP: 暗号的証明によるトークン保護
```

#### シナリオ3: 認可要求改ざん攻撃

```
攻撃フロー:
1. [Client] → 認可リクエスト → [攻撃者]
2. [攻撃者] → パラメータ改ざん（scope拡大など）
3. [攻撃者] → 改ざんリクエスト → [認可サーバー]

FAPI対策:
- JAR (JWT Secured Authorization Request)
- リクエストオブジェクトの署名検証
- PAR (バックチャンネル送信)
```

### 4.3 攻撃者モデルに基づく脅威分析

```
脅威マトリックス:
                    A1(Network) A2(Web) A3(App)
認証情報の盗聴         高        中      低
セッション固定         中        高      中
認可コード横取り       中        高      高
トークン不正使用       高        中      高
リクエスト改ざん       高        中      中
```

## 5. 実装アドバイス

### 5.1 セキュリティ実装のベストプラクティス

#### 暗号化実装
```python
# 推奨: 検証済みライブラリの使用
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import serialization

class FAPISecurityManager:
    def __init__(self):
        # RSA 2048bit以上を推奨
        self.private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048
        )
    
    def sign_request(self, payload):
        """JARのための署名生成"""
        # 署名アルゴリズムはRS256, ES256を推奨
        signature = self.private_key.sign(
            payload,
            padding.PSS(
                mgf=padding.MGF1(hashes.SHA256()),
                salt_length=padding.PSS.MAX_LENGTH
            ),
            hashes.SHA256()
        )
        return signature
```

#### 鍵管理
```yaml
# 鍵管理のベストプラクティス
key_management:
  storage:
    - HSM推奨（FIPS 140-2 Level 2以上）
    - 環境変数での保存は避ける
    - 専用の鍵管理サービス使用
  
  rotation:
    - 定期的なローテーション（90日推奨）
    - 段階的な移行期間の設定
    - 古い鍵の適切な廃棄
  
  access_control:
    - 最小権限の原則
    - 監査ログの記録
    - 多要素認証での保護
```

### 5.2 PAR (Pushed Authorization Request) の実装

```python
import httpx
import jwt
from datetime import datetime, timedelta

class PARClient:
    def __init__(self, client_id, private_key, par_endpoint):
        self.client_id = client_id
        self.private_key = private_key
        self.par_endpoint = par_endpoint
    
    async def push_authorization_request(self, 
                                       redirect_uri, 
                                       scope, 
                                       state,
                                       code_challenge):
        """PARエンドポイントへの認可リクエスト送信"""
        
        # JARの作成
        now = datetime.utcnow()
        request_object = {
            "iss": self.client_id,
            "aud": self.par_endpoint,
            "exp": now + timedelta(minutes=5),
            "iat": now,
            "nbf": now,
            "response_type": "code",
            "client_id": self.client_id,
            "redirect_uri": redirect_uri,
            "scope": scope,
            "state": state,
            "nonce": generate_nonce(),
            "code_challenge": code_challenge,
            "code_challenge_method": "S256"
        }
        
        # JWT署名
        signed_request = jwt.encode(
            request_object, 
            self.private_key, 
            algorithm="RS256",
            headers={"kid": self.key_id}
        )
        
        # PARリクエスト送信
        async with httpx.AsyncClient() as client:
            response = await client.post(
                self.par_endpoint,
                data={
                    "client_id": self.client_id,
                    "request": signed_request
                },
                headers={
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            )
            
        if response.status_code != 201:
            raise Exception(f"PAR failed: {response.text}")
            
        par_response = response.json()
        return par_response["request_uri"]
```

### 5.3 MTLS実装

```python
import ssl
from pathlib import Path

class MTLSClient:
    def __init__(self, client_cert_path, client_key_path, ca_cert_path):
        self.client_cert = client_cert_path
        self.client_key = client_key_path
        self.ca_cert = ca_cert_path
        
    def create_mtls_context(self):
        """MTLS用のSSLコンテキスト作成"""
        context = ssl.create_default_context(
            purpose=ssl.Purpose.CLIENT_AUTH,
            cafile=self.ca_cert
        )
        
        # クライアント証明書の設定
        context.load_cert_chain(
            certfile=self.client_cert,
            keyfile=self.client_key
        )
        
        # TLS 1.2以上を強制
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        
        # 推奨される暗号スイートのみ許可
        context.set_ciphers(
            'ECDHE+AESGCM:ECDHE+CHACHA20:DHE+AESGCM:DHE+CHACHA20:!aNULL:!MD5:!DSS'
        )
        
        return context
    
    async def make_authenticated_request(self, url, data):
        """MTLS認証付きリクエスト"""
        ssl_context = self.create_mtls_context()
        
        async with httpx.AsyncClient(verify=ssl_context) as client:
            response = await client.post(url, json=data)
            return response
```

### 5.4 DPoP実装

```python
import time
import uuid
from jose import jwk, jwt
from cryptography.hazmat.primitives.asymmetric import ec

class DPoPManager:
    def __init__(self):
        # ECDSAキーペアの生成
        self.private_key = ec.generate_private_key(ec.SECP256R1())
        self.public_key = self.private_key.public_key()
        
    def create_dpop_proof(self, http_method, uri, access_token=None):
        """DPoP証明の生成"""
        
        # JWKサムプリント計算
        jwk_dict = {
            "kty": "EC",
            "crv": "P-256",
            "x": base64url_encode(self.public_key.x),
            "y": base64url_encode(self.public_key.y)
        }
        
        header = {
            "typ": "dpop+jwt",
            "alg": "ES256",
            "jwk": jwk_dict
        }
        
        payload = {
            "jti": str(uuid.uuid4()),
            "htm": http_method,
            "htu": uri,
            "iat": int(time.time()),
            "exp": int(time.time()) + 60
        }
        
        # アクセストークンがある場合はathクレームを追加
        if access_token:
            payload["ath"] = base64url_encode(
                hashlib.sha256(access_token.encode()).digest()
            )
        
        # DPoP証明の署名
        dpop_proof = jwt.encode(
            payload, 
            self.private_key, 
            algorithm="ES256",
            headers=header
        )
        
        return dpop_proof
```

### 5.5 エラーハンドリングとロギング

```python
import logging
import json
from datetime import datetime

class FAPIErrorHandler:
    def __init__(self):
        self.logger = self._setup_logger()
    
    def _setup_logger(self):
        """セキュアなロギング設定"""
        logger = logging.getLogger('fapi_security')
        handler = logging.FileHandler('fapi_audit.log')
        
        # 構造化ログフォーマット
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)
        
        return logger
    
    def handle_auth_error(self, error_type, details=None):
        """認証エラーの適切な処理"""
        
        # センシティブ情報をマスク
        safe_details = self._mask_sensitive_info(details)
        
        # 構造化ログ記録
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "event_type": "auth_error",
            "error_type": error_type,
            "details": safe_details,
            "correlation_id": str(uuid.uuid4())
        }
        
        self.logger.error(json.dumps(log_entry))
        
        # クライアントへの応答（詳細を隠蔽）
        if error_type == "invalid_client":
            return {
                "error": "invalid_client",
                "error_description": "Client authentication failed"
            }, 401
        elif error_type == "invalid_grant":
            return {
                "error": "invalid_grant",
                "error_description": "The provided authorization grant is invalid"
            }, 400
        else:
            return {
                "error": "server_error",
                "error_description": "The authorization server encountered an error"
            }, 500
    
    def _mask_sensitive_info(self, data):
        """センシティブ情報のマスキング"""
        if not data:
            return data
            
        sensitive_fields = ['password', 'client_secret', 'access_token', 'refresh_token']
        masked_data = data.copy() if isinstance(data, dict) else {}
        
        for field in sensitive_fields:
            if field in masked_data:
                masked_data[field] = "***MASKED***"
                
        return masked_data
```

### 5.6 パフォーマンス最適化

```python
import asyncio
from functools import lru_cache
from datetime import datetime, timedelta

class FAPIPerformanceOptimizer:
    def __init__(self):
        self.cache = {}
        self.jwks_cache = {}
        
    @lru_cache(maxsize=1000)
    def verify_token_cached(self, token, public_key):
        """トークン検証結果のキャッシング"""
        try:
            # トークン検証（計算コストが高い）
            payload = jwt.decode(
                token, 
                public_key, 
                algorithms=['RS256'],
                options={"verify_aud": False}
            )
            return True, payload
        except Exception as e:
            return False, str(e)
    
    async def get_jwks_with_cache(self, jwks_uri):
        """JWKSエンドポイントのキャッシング"""
        
        # キャッシュチェック
        if jwks_uri in self.jwks_cache:
            cached_data, expiry = self.jwks_cache[jwks_uri]
            if datetime.utcnow() < expiry:
                return cached_data
        
        # JWKSの取得
        async with httpx.AsyncClient() as client:
            response = await client.get(jwks_uri)
            jwks_data = response.json()
        
        # キャッシュに保存（1時間）
        self.jwks_cache[jwks_uri] = (
            jwks_data,
            datetime.utcnow() + timedelta(hours=1)
        )
        
        return jwks_data
    
    async def parallel_token_validation(self, tokens, public_key):
        """複数トークンの並列検証"""
        tasks = [
            self.verify_token_async(token, public_key) 
            for token in tokens
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
```

### 5.7 監視とアラート

```yaml
# 監視項目の設定例
monitoring:
  metrics:
    - name: auth_request_rate
      threshold: 1000/min
      alert: rate_limit_warning
      
    - name: failed_auth_rate
      threshold: 10%
      alert: security_alert
      
    - name: token_validation_latency
      threshold: 100ms
      alert: performance_degradation
      
    - name: par_endpoint_availability
      threshold: 99.9%
      alert: service_degradation

  alerts:
    - type: security_alert
      channels: [email, slack, pagerduty]
      severity: high
      
    - type: performance_degradation
      channels: [email, slack]
      severity: medium

  dashboards:
    - auth_flow_metrics
    - security_events
    - api_performance
    - error_rates
```

## 6. FAPIと各国標準の補完的関係

### 6.1 Open Banking UK との関係

Open Banking UKは、FAPIをベースに英国市場向けの追加要件を定義しています：

#### 技術スタック
```
Open Banking UK Standards
         ↓
    FAPI 1.0 Advanced
         ↓
    OpenID Connect
         ↓
      OAuth 2.0
```

#### 追加要件
- **Customer Experience Guidelines**: ユーザー体験の標準化
- **Operational Guidelines**: 運用要件とSLA
- **Read/Write Data Standards**: APIペイロードの標準化
- **Directory Standards**: 参加者の登録と管理

### 6.2 全銀協オープンAPI仕様との関係

全銀協のオープンAPI仕様は、FAPIを日本の金融環境に適応させたものです：

#### 実装レベル
```
レベル1（参照系API）
├── FAPI 1.0 Baseline準拠
├── 読み取り専用アクセス
└── 残高照会、取引明細照会

レベル2（更新系API）
├── FAPI 1.0 Advanced準拠
├── 更新・決済機能
└── 振込、振替、定期預金
```

#### 日本特有の要件
- 文字エンコーディング（UTF-8）の明確化
- 日本語エラーメッセージの標準化
- 金融規制（銀行法、資金決済法）への準拠
- 全銀フォーマットとの互換性

### 6.3 グローバル相互運用性

```
         FAPI (Global Standard)
              ↓
    ┌─────────┼─────────┐
    ↓         ↓         ↓
UK Open   全銀協API   EU PSD2
Banking               Standards
    ↓         ↓         ↓
 UK Banks  JP Banks  EU Banks
    └─────────┴─────────┘
              ↓
    Cross-border Interoperability
```

## 7. 今後の展望

### 7.1 技術的進化
- **FAPI 2.0の普及**: シンプルで実装しやすい統一プロファイル
- **新認証技術の統合**: WebAuthn、パスキーとの連携
- **分散型ID**: Verifiable Credentialsの活用
- **量子耐性暗号**: ポスト量子暗号への移行準備

### 7.2 ビジネス展開
- **オープンファイナンス**: 銀行以外の金融サービスへの拡大
- **組込み金融**: 非金融サービスへのAPI組込み
- **国際標準化**: ISO/IECでの標準化推進
- **新興市場**: アジア・アフリカでの採用加速

## まとめ

FAPIは金融グレードのセキュリティを実現するための包括的なフレームワークです：

1. **強固なセキュリティ**: 多層防御による高度な保護
2. **標準準拠**: OAuth 2.0/OIDCとの完全な互換性
3. **実装の柔軟性**: ユースケースに応じた選択
4. **将来性**: 継続的な進化と改善

金融APIを実装する際は、FAPIの採用を強く推奨します。セキュリティプロファイル、攻撃者モデル、実装アドバイスの3つの構成要素を理解し、適切に実装することで、安全で信頼性の高い金融サービスを提供できます。