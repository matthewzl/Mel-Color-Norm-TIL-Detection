function [HE, maxC] = getReferenceData(I, Io, beta, alpha)

% transmitted light intensity
if ~exist('Io', 'var') || isempty(Io)
    Io = 240;
    % fprintf('Io: %d \n',Io)
end

% OD threshold for transparent pixels
if ~exist('beta', 'var') || isempty(beta)
    beta = 0.15;
    % fprintf('Beta: %d \n',beta)
end

% tolerance for the pseudo-min and pseudo-max
if ~exist('alpha', 'var') || isempty(alpha)
    alpha = 1;
    % fprintf('Alpha: %d \n',alpha)
end

% % reference H&E OD matrix
% if ~exist('HERef', 'var') || isempty(HERef)
%     HERef = [
%         0.5626    0.2159
%         0.7201    0.8012
%         0.4062    0.5581
%         ];
% end

I = double(I);

I = reshape(I, [], 3);


% calculate optical density
OD = -log((I+1)/Io); % maybe get this parameter ML
% fprintf('OD: %d \n', OD)

% remove transparent pixels
ODhat = OD(~any(OD < beta, 2), :);

% calculate eigenvectors
[V, ~] = eig(cov(ODhat));

% project on the plane spanned by the eigenvectors corresponding to the two
% largest eigenvalues
That = ODhat*V(:,2:3);

% find the min and max vectors and project back to OD space
phi = atan2(That(:,2), That(:,1));

minPhi = prctile(phi, alpha);
maxPhi = prctile(phi, 100-alpha);

vMin = V(:,2:3)*[cos(minPhi); sin(minPhi)];
vMax = V(:,2:3)*[cos(maxPhi); sin(maxPhi)];

% a heuristic to make the vector corresponding to hematoxylin first and the
% one corresponding to eosin second
if vMin(1) > vMax(1)
    HE = [vMin vMax];
else
    HE = [vMax vMin];
end

% rows correspond to channels (RGB), columns to OD values
Y = reshape(OD, [], 3)';

% determine concentrations of the individual stains
C = HE \ Y;

% normalize stain concentrations
maxC = prctile(C, 99, 2);