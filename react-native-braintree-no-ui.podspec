require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'react-native-braintree-no-ui'
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "14.0"

  s.source       = { :git => "https://github.com/StronkMan/react-native-braintree-no-ui.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m}"

  s.dependency 'Braintree', '6.18.0'
  s.dependency 'Braintree/PayPal', '6.18.0'
  s.dependency 'Braintree/Venmo', '6.18.0'
  s.dependency 'Braintree/ApplePay', '6.18.0'
  s.dependency 'Braintree/ThreeDSecure', '6.18.0'
  s.dependency 'Braintree/DataCollector', '6.18.0'
  s.dependency 'React'
end
